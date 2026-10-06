// Drive JetBrains ILS (kotlin-lsp) over stdio against the warmed index cache:
// initialize -> initialized -> didOpen -> wait intellij/ready-for-test ->
// documentSymbol (analysis check) -> textDocument/diagnostic -> shutdown.
//
// Usage:
//   node scripts/kotlin-lsp-diagnostics.js <absolute-file-path>
//
// Machine specifics stay out of this repo. The ILS installation is resolved
// from, in order:
//   KOTLIN_LSP_SERVER  full path to intellij-server(.exe) (explicit override)
//   KOTLIN_LSP_HOME    distribution root (the conventional user-level
//                      environment variable); <root>/bin/intellij-server is used
//   PATH               the intellij-server binary found on PATH
// The ILS index cache is expected at <repo>/.kotlin/lsp-cache (gitignored) --
// warm it once with the distribution's bin/warmup.py when missing.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const target = path.resolve(process.argv[2]);
if (!fs.existsSync(target)) {
  console.error('file not found: ' + target);
  process.exit(1);
}
const repoRoot = path.join(__dirname, '..');
const indexDir = path.join(repoRoot, '.kotlin', 'lsp-cache');
const serverExe = 'intellij-server' + (process.platform === 'win32' ? '.exe' : '');
function serverFromPathEnv() {
  for (const dir of (process.env.PATH || '').split(path.delimiter)) {
    if (!dir) continue;
    const candidate = path.join(dir, serverExe);
    if (fs.existsSync(candidate)) return candidate;
  }
  return undefined;
}
const server = process.env.KOTLIN_LSP_SERVER
  || (process.env.KOTLIN_LSP_HOME && path.join(process.env.KOTLIN_LSP_HOME, 'bin', serverExe))
  || serverFromPathEnv();
if (!server || !fs.existsSync(server)) {
  console.error('ILS server not found. Set KOTLIN_LSP_SERVER (binary path) or KOTLIN_LSP_HOME (distribution root, the conventional user-level variable), or put intellij-server on PATH');
  process.exit(1);
}

const fileUri = 'file:///' + target.replace(/\\/g, '/');
const projectUri = 'file:///' + repoRoot.replace(/\\/g, '/');

const proc = spawn(server, ['--stdio', '--system-path', indexDir], {
  cwd: repoRoot,
  stdio: ['pipe', 'pipe', 'pipe'],
});

let buf = Buffer.alloc(0);
const pending = new Map();
let nextId = 1;
let ready = false;
proc.stdout.on('data', (d) => {
  buf = Buffer.concat([buf, d]);
  pump();
});
proc.stderr.on('data', (d) => process.stderr.write('[ils] ' + d.toString()));

function pump() {
  for (;;) {
    const headerEnd = buf.indexOf('\r\n\r\n');
    if (headerEnd < 0) return;
    const header = buf.slice(0, headerEnd).toString('ascii');
    const m = /Content-Length:\s*(\d+)/i.exec(header);
    if (!m) { buf = buf.slice(headerEnd + 4); continue; }
    const len = parseInt(m[1], 10);
    if (buf.length < headerEnd + 4 + len) return;
    const body = buf.slice(headerEnd + 4, headerEnd + 4 + len).toString('utf8');
    buf = buf.slice(headerEnd + 4 + len);
    handle(JSON.parse(body));
  }
}

function handle(msg) {
  if (msg.id !== undefined && pending.has(msg.id)) {
    const { resolve, timer } = pending.get(msg.id);
    clearTimeout(timer);
    pending.delete(msg.id);
    resolve(msg.error ? { error: msg.error } : msg.result);
    return;
  }
  if (msg.method !== undefined && msg.id !== undefined) {
    // server -> client request: warmup.py answers result:null across the board;
    // ignoring these stalls the handshake
    reply(msg.id, null);
  }
  if (msg.method === 'intellij/ready-for-test') {
    console.log('== ready-for-test received ==');
    ready = true;
  }
}

function reply(id, result) {
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, result }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
}

function send(method, params) {
  const id = nextId++;
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, method, params }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(id);
      reject(new Error('timeout waiting for ' + method));
    }, 180000);
    pending.set(id, { resolve, timer });
  });
}

function notify(method, params) {
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', method, params }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
}

function waitForReady(ms) {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const t = setInterval(() => {
      if (ready) { clearInterval(t); resolve(); }
      else if (Date.now() - start > ms) { clearInterval(t); reject(new Error('ready-for-test timeout')); }
    }, 500);
  });
}

(async () => {
  try {
    const init = await send('initialize', {
      processId: process.pid,
      clientInfo: { name: 'ils-diagnostics' },
      rootUri: projectUri,
      rootPath: repoRoot,
      workspaceFolders: [{ uri: projectUri, name: path.basename(repoRoot) }],
      capabilities: { window: { workDoneProgress: true }, textDocument: { diagnostic: {} } },
      initializationOptions: { indexDir },
    });
    console.log('== initialized, server:', init.serverInfo && init.serverInfo.name, init.serverInfo && init.serverInfo.version);
    notify('initialized', {});
    notify('textDocument/didOpen', {
      textDocument: { uri: fileUri, languageId: 'kotlin', version: 1, text: fs.readFileSync(target, 'utf8') },
    });

    await waitForReady(420000);

    const symbols = await send('textDocument/documentSymbol', { textDocument: { uri: fileUri } });
    console.log('== documentSymbol count:', Array.isArray(symbols) ? symbols.length : String(JSON.stringify(symbols)).slice(0, 200));

    const diag = await send('textDocument/diagnostic', { textDocument: { uri: fileUri } });
    console.log('== diagnostics for', target);
    const items = (diag && diag.items) || [];
    if (items.length === 0) console.log('CLEAN (0 items)');
    for (const d of items) {
      const pos = d.range && d.range.start;
      const sev = d.severity === 1 ? 'ERROR' : d.severity === 2 ? 'WARN' : 'INFO';
      console.log(`[${sev}] ${pos ? pos.line + 1 + ':' + (pos.character + 1) : '?'} ${d.code || ''} ${String(d.message).split('\n')[0]}`);
    }

    const shutdown = await send('shutdown', {});
    notify('exit', {});
    console.log('== shutdown ack:', JSON.stringify(shutdown));
    setTimeout(() => process.exit(0), 1500);
  } catch (e) {
    console.error('FAILED:', e.message);
    proc.kill();
    process.exit(1);
  }
})();
