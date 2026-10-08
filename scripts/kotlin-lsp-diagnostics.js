// Drive JetBrains ILS (kotlin-lsp) over stdio against the warmed index cache:
// initialize -> initialized -> didOpen -> wait intellij/ready-for-test ->
// documentSymbol (analysis check) -> textDocument/diagnostic -> shutdown.
//
// Usage:
//   node scripts/kotlin-lsp-diagnostics.js <path-to-kotlin-file>
//
// .kt files are analyzed; a .kts Gradle build script has no ILS model, so a run
// on one fails by design instead of reporting an empty pass.
//
// Status: 0 only when the pull succeeded and no ERROR-severity item came back;
// 1 for a failed run (server, handshake, unanalyzed document) or any ERROR item;
// 130 or 143 when interrupted by SIGINT or SIGTERM, after stopping the server.
// A second run started while one is in progress exits 1 naming the first pid.
//
// Machine specifics stay out of this repo. The ILS installation is resolved
// from, in order:
//   KOTLIN_LSP_SERVER  full path to intellij-server(.exe) (explicit override)
//   KOTLIN_LSP_HOME    distribution root (the conventional user-level
//                      environment variable); <root>/bin/intellij-server is used
//   PATH               the intellij-server binary found on PATH
// The ILS index cache is expected at <repo>/.kotlin/lsp-cache/<process.platform>
// (gitignored): each distribution, and the cache that distribution writes, is
// platform-specific, so every platform warms its own directory with its own
// bin/warmup.py.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');

if (!process.argv[2]) {
  console.error('usage: node scripts/kotlin-lsp-diagnostics.js <path-to-kotlin-file>');
  process.exit(1);
}
const target = path.resolve(process.argv[2]);
if (!fs.existsSync(target)) {
  console.error('file not found: ' + target);
  process.exit(1);
}
if (!/\.(kt|kts)$/i.test(target)) {
  // The document is opened as languageId "kotlin"; any other file gets analyzed
  // as Kotlin source and produces results that mean nothing for it.
  console.error('not a Kotlin source file (expected .kt or .kts): ' + target);
  process.exit(1);
}
const repoRoot = path.join(__dirname, '..');
// Per platform: warmup.py hands this directory to the server as --system-path,
// so it becomes IntelliJ's system dir, which only the launcher and JBR that
// wrote it can reuse.
const indexDir = path.join(repoRoot, '.kotlin', 'lsp-cache', process.platform);
if (!fs.existsSync(indexDir)) {
  console.log('WARN: no index cache at ' + indexDir + ' -- this run cold-indexes and may exceed'
    + ' the ready timeout; warm it with <ILS distribution>/bin/warmup.py first');
}
const serverExe = 'intellij-server' + (process.platform === 'win32' ? '.exe' : '');
// X_OK is granted to every file on Windows, so this only ever rejects on the
// POSIX platforms -- where an archive extracted without the execute bit would
// otherwise reach spawn() and fail there with a far less specific error.
function isRunnable(p) {
  if (!fs.existsSync(p)) return false;
  try { fs.accessSync(p, fs.constants.X_OK); } catch { return false; }
  return true;
}
function serverFromPathEnv() {
  for (const dir of (process.env.PATH || '').split(path.delimiter)) {
    if (!dir) continue;
    const candidate = path.join(dir, serverExe);
    if (isRunnable(candidate)) return candidate;
  }
  return undefined;
}
const server = process.env.KOTLIN_LSP_SERVER
  || (process.env.KOTLIN_LSP_HOME && path.join(process.env.KOTLIN_LSP_HOME, 'bin', serverExe))
  || serverFromPathEnv();
if (!server) {
  console.error('ILS server not found. Set KOTLIN_LSP_SERVER (binary path) or KOTLIN_LSP_HOME (distribution root, the conventional user-level variable), or put intellij-server on PATH');
  process.exit(1);
}
if (!fs.existsSync(server)) {
  console.error('ILS server not found at ' + server + ' (KOTLIN_LSP_SERVER / KOTLIN_LSP_HOME point there);'
    + ' note that each platform ships its own distribution, whose launcher is bin/' + serverExe);
  process.exit(1);
}
if (!isRunnable(server)) {
  console.error('ILS server ' + server + ' exists but is not runnable: on macOS/Linux check the execute bit'
    + ' (chmod +x) and, on macOS, that the download is not quarantined');
  process.exit(1);
}

const fileUri = pathToFileURL(target).href;
const projectUri = pathToFileURL(repoRoot).href;

// Two instances against one platform cache contend for the index, and the run
// that loses fails in a way that looks like a code bug. The lock is created
// atomically (flag wx) rather than checked then written, because two runs
// starting together both pass a check-then-write. It only covers this script:
// an IDE-hosted ILS and warmup.py write no lock for it to see.
const lockFile = path.join(repoRoot, '.kotlin', 'lsp-cache', process.platform + '.lock');
function liveLockHolderPid() {
  let pid;
  try { pid = parseInt(fs.readFileSync(lockFile, 'utf8'), 10); } catch { return 0; }
  if (!pid) return 0;
  try { process.kill(pid, 0); return pid; }
  // EPERM: the pid exists but is not ours to signal, so it is still a live run
  catch (e) { return e.code === 'EPERM' ? pid : 0; }
}
fs.mkdirSync(path.dirname(lockFile), { recursive: true });
let lockHeld = false;
function acquireLock() {
  try {
    fs.writeFileSync(lockFile, String(process.pid), { flag: 'wx' });
    lockHeld = true;
    return;
  } catch (e) {
    if (e.code !== 'EEXIST') {
      console.error('cannot write the diagnostics lock ' + lockFile + ': ' + e.message);
      process.exit(1);
    }
    const holder = liveLockHolderPid();
    if (holder) {
      console.error('another diagnostics run is already in progress (pid ' + holder + ', lock ' + lockFile + ')');
      process.exit(1);
    }
    // A lock left behind by a run that was killed hard: take it over once
    try {
      fs.unlinkSync(lockFile);
      fs.writeFileSync(lockFile, String(process.pid), { flag: 'wx' });
      lockHeld = true;
      return;
    } catch (e2) {
      console.error('cannot take over the stale diagnostics lock ' + lockFile + ': ' + e2.message);
      process.exit(1);
    }
  }
}
acquireLock();
// Reaches every exit path that runs handlers at all: the success tail, the
// catch above, and the signal handlers below.
process.on('exit', () => {
  if (lockHeld) {
    try { fs.unlinkSync(lockFile); } catch { /* already gone */ }
  }
});

// The distribution's own warmup.py budgets 1800s of wall clock before it force
// kills the server, and that is the order of magnitude a cold index on a fresh
// platform needs, so the ready wait matches it instead of the Windows warm-cache
// timing. Both are overridable in milliseconds for a machine that needs more.
function timeoutFromEnv(name, fallback) {
  const raw = Number(process.env[name]);
  return Number.isFinite(raw) && raw > 0 ? raw : fallback;
}
const readyTimeoutMs = timeoutFromEnv('ILS_READY_TIMEOUT_MS', 1800000);
const callTimeoutMs = timeoutFromEnv('ILS_CALL_TIMEOUT_MS', 180000);

// spawn() fails two different ways depending on the platform and the cause: a
// synchronous throw (e.g. Windows EFTYPE on a non-executable file) and an
// asynchronous 'error' event (ENOENT/EACCES), so both need a guard.
let proc;
try {
  proc = spawn(server, ['--stdio', '--system-path', indexDir], {
    cwd: repoRoot,
    stdio: ['pipe', 'pipe', 'pipe'],
  });
} catch (e) {
  console.error('cannot start ILS (' + e.code + '): ' + e.message);
  process.exit(1);
}

// Without a handler Node stops on SIGINT/SIGTERM with no cleanup: the server it
// already started keeps running and holds the index cache and the lock. The
// status follows the shell convention of 128 + signal number. Exit also drops
// the lock through the 'exit' handler.
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    console.error('interrupted by ' + signal + ': stopping the ILS process');
    proc.kill();
    process.exit(signal === 'SIGINT' ? 130 : 143);
  });
}

let buf = Buffer.alloc(0);
const pending = new Map();
let nextId = 1;
let ready = false;
let exitInfo = undefined;
// An exited server has to fail the run at once instead of leaving every caller
// on its timeout: writes to its stdin are dropped silently (no throw, no
// 'error' event), so a dead pipe gives no other signal.
function markClosed(info) {
  if (exitInfo) return;
  exitInfo = info;
  for (const { reject, timer, method } of pending.values()) {
    clearTimeout(timer);
    reject(new Error(method + ': ILS ' + info));
  }
  pending.clear();
}
// Node reports a failed spawn either synchronously or through this event, and
// can also raise it for a process that started fine, so it is handled as a
// loss of the server rather than as "never started".
proc.on('error', (e) => markClosed('errored with ' + e.code
  + (e.code === 'EACCES' ? ' (not runnable: check the execute bit, chmod +x)' : '')));
proc.stdout.on('data', (d) => {
  buf = Buffer.concat([buf, d]);
  pump();
});
proc.stderr.on('data', (d) => process.stderr.write('[ils] ' + d.toString()));
proc.on('close', (code, signal) => markClosed('exited with code ' + code
  + (signal ? ' on signal ' + signal : '')));
proc.stdin.on('error', () => {});

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
    try {
      handle(JSON.parse(body));
    } catch (e) {
      // A frame that is not a JSON object (a stray or truncated write) must not
      // take the run down as an uncaught throw inside the stdout listener: that
      // skips the shutdown tail entirely and leaves the server holding the index
      // cache locked for the next run.
      console.error('ignoring unprocessable frame: ' + e.message + ' -- ' + body.slice(0, 120));
    }
  }
}

function handle(msg) {
  // Dispatch on the JSON-RPC shape first: a message carrying a method is a
  // request or notification from the server, one carrying only an id is a
  // response to a call we made. Checking the pending map before this let a
  // server -> client request settle a pending call whenever the two
  // independently picked the same id, since client and server number from one.
  if (msg.method !== undefined) {
    if (msg.id !== undefined) {
      // server -> client request: warmup.py answers result:null across the board;
      // ignoring these stalls the handshake
      reply(msg.id, null);
    }
    if (msg.method === 'intellij/ready-for-test') {
      console.log('== ready-for-test received ==');
      ready = true;
    }
    return;
  }
  if (msg.id !== undefined && pending.has(msg.id)) {
    const { resolve, reject, timer, method } = pending.get(msg.id);
    clearTimeout(timer);
    pending.delete(msg.id);
    // A JSON-RPC error is a failed call, not a value: resolving it let a
    // rejected request fall through to the branches that print CLEAN.
    if (msg.error) reject(new Error(method + ': ILS error ' + msg.error.code + ' ' + msg.error.message));
    else resolve(msg.result);
  }
}

function reply(id, result) {
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, result }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
}

function send(method, params) {
  // A request written after the server was already gone is silently dropped by
  // the pipe, so without this it would sit out its full timeout.
  if (exitInfo) throw new Error('cannot send ' + method + ': ILS already ' + exitInfo);
  const id = nextId++;
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, method, params }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(id);
      reject(new Error('timeout waiting for ' + method + ' after ' + callTimeoutMs + 'ms'));
    }, callTimeoutMs);
    pending.set(id, { resolve, reject, timer, method });
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
      else if (exitInfo) { clearInterval(t); reject(new Error('ILS ' + exitInfo + ' before becoming ready')); }
      else if (Date.now() - start > ms) { clearInterval(t); reject(new Error('ready-for-test timeout after ' + ms + 'ms')); }
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
      // Same map form as the distribution's warmup.py:107-109: it pins the
      // workspace importer to Gradle rather than leaving the choice to the
      // server default, matching how the index cache here is warmed.
      initializationOptions: { indexDir, buildTools: { [projectUri]: 'gradle' } },
    });
    console.log('== initialized, server:', init.serverInfo && init.serverInfo.name, init.serverInfo && init.serverInfo.version);
    notify('initialized', {});
    notify('textDocument/didOpen', {
      textDocument: { uri: fileUri, languageId: 'kotlin', version: 1, text: fs.readFileSync(target, 'utf8') },
    });

    await waitForReady(readyTimeoutMs);

    const symbols = await send('textDocument/documentSymbol', { textDocument: { uri: fileUri } });
    console.log('== documentSymbol count:',
      Array.isArray(symbols) ? symbols.length : String(JSON.stringify(symbols)).slice(0, 200));
    // documentSymbol is the "is it really analyzing" check: a document with no
    // model also yields an empty diagnostic report, so without this the run
    // would print CLEAN for a file that was never analyzed.
    if (!Array.isArray(symbols) || symbols.length === 0) {
      const why = /\.kts$/i.test(target)
        ? 'ILS resolves no Gradle build script here, so the file was not analyzed: check it with the Gradle build instead'
        : 'the document is not part of the project model (path or URI not recognised, or the index cache is missing)';
      throw new Error('documentSymbol returned '
        + (Array.isArray(symbols) ? '0 symbols' : String(JSON.stringify(symbols)))
        + ' for ' + target + ': ' + why);
    }

    const diag = await send('textDocument/diagnostic', { textDocument: { uri: fileUri } });
    console.log('== diagnostics for', target);
    if (!diag || !Array.isArray(diag.items)) {
      throw new Error('no diagnostic report for ' + target + ' (got ' + String(JSON.stringify(diag)) + ')');
    }
    const items = diag.items;
    if (items.length === 0) console.log('CLEAN (0 items)');
    const counts = { ERROR: 0, WARN: 0, INFO: 0 };
    for (const d of items) {
      const pos = d.range && d.range.start;
      const sev = d.severity === 1 ? 'ERROR' : d.severity === 2 ? 'WARN' : 'INFO';
      counts[sev]++;
      console.log(`[${sev}] ${pos ? pos.line + 1 + ':' + (pos.character + 1) : '?'} ${d.code || ''} ${String(d.message).split('\n')[0]}`);
    }
    // Pulling the report successfully is not the same as the file being fine, so
    // an ERROR-severity item has to show up in the status rather than only in
    // stdout, which every caller would have to parse to use this as a gate.
    if (items.length > 0) console.log(`== ${counts.ERROR} ERROR, ${counts.WARN} WARN, ${counts.INFO} INFO ==`);

    const shutdown = await send('shutdown', {});
    notify('exit', {});
    console.log('== shutdown ack:', JSON.stringify(shutdown));
    // Give the graceful exit a deadline rather than killing on a fixed timer:
    // a kill landing while the JVM flushes its cache locks it for the next run,
    // which is the same sequence the distribution's own warmup.py follows
    // (shutdown + exit, wait 10s, only then force-kill).
    await new Promise((resolve) => {
      const timer = setTimeout(() => { proc.kill(); resolve(); }, 10000);
      proc.once('close', () => { clearTimeout(timer); resolve(); });
    });
    process.exit(counts.ERROR > 0 ? 1 : 0);
  } catch (e) {
    console.error('FAILED:', e.message);
    proc.kill();
    process.exit(1);
  }
})();
