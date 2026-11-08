// Drive the JetBrains Kotlin LSP for the EDIT-producing operations, the
// write-side counterpart of kotlin-lsp-plan.js (which stays read-only):
//
//   node scripts/kotlin-lsp-edit.js format <file> [--apply]
//   node scripts/kotlin-lsp-edit.js organize-imports <file> [--apply]
//
// Every op only COMPUTES edits unless --apply is given: the returned
// WorkspaceEdit / TextEdits are printed per file (range and the new text,
// trimmed), and --apply writes them. organize-imports is applyEdit-gated: the
// edits arrive as a server->client workspace/applyEdit request, this script
// captures that edit, answers the server itself, and never lets the server
// write.
//
// Deliberately NOT wired, probed on ILS-263.7084.0 (evidence in
// 10-分析细则.md):
// - rename / will-rename: the rename computation is inconsistent on this
//   build. A class rename returned clean zero-width insertions at every
//   occurrence; local-variable renames returned shredded garbage edit sets
//   (one even hitting the import block) or a plain refusal (-32602 "This
//   element cannot be renamed"). The source confirms the edits are a
//   CHARACTER-granularity document diff, so a wrong baseline shreds the text.
//   An op that cannot say when it is right is a trap; it returns to the
//   toolbox when the server build fixes it.
// - quickfix: codeAction with diagnostics needs a diagnostic, and the
//   diagnostic sources are unreliable in scripted sessions (the pull answers
//   empty while inspections are still running; publishDiagnostics for
//   inspections arrives late or never; an empty diagnostics context yields
//   only the organize-imports action).
// inlay hints ARE served elsewhere: they died on our own null reply to the
// server's workspace/configuration request, and once the client answers with
// an array (kotlin-lsp-plan.js carries that handler and the query), they work.
//
// Line endings: edits are applied against the text read from disk, so CRLF
// working copies keep their CRLF, and an out-of-line position clamps before
// the carriage return; always check `git diff` after an --apply run.
//
// Exit code: 0 for a successful run (including one that found no edits), 1 for
// a failed run (server, handshake, no project model), 130 or 143 when
// interrupted, like the other two channels.
//
// The lock, server resolution, timeouts and the exit-code contract are the
// shared ones from kotlin-lsp-plan.js / kotlin-lsp-diagnostics.js: two server
// instances against one platform cache contend for the index, so all three
// scripts are mutually exclusive through the same lock file.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const { pathToFileURL, fileURLToPath } = require('url');

const OPS = ['format', 'organize-imports'];

if (process.argv.length < 3) {
  console.error('usage: node scripts/kotlin-lsp-edit.js <op> [args] [--apply]');
  console.error('op: ' + OPS.join(', '));
  console.error('  format <file>');
  console.error('  organize-imports <file>');
  console.error('(rename / will-rename are deliberately not offered; see the header)');
  process.exit(1);
}

const apply = process.argv.includes('--apply');
const argv = process.argv.slice(2).filter(a => a !== '--apply');
const op = argv[0];
if (!OPS.includes(op)) {
  console.error('unknown op "' + op + '"; ops: ' + OPS.join(', '));
  process.exit(1);
}

const repoRoot = path.join(__dirname, '..');
const indexDir = path.join(repoRoot, '.kotlin', 'lsp-cache', process.platform);
if (!fs.existsSync(indexDir)) {
  console.log('WARN: no index cache at ' + indexDir + ' -- this run cold-indexes and may exceed'
    + ' the ready timeout; warm it with <distribution>/bin/warmup.py first');
}
const serverExe = 'intellij-server' + (process.platform === 'win32' ? '.exe' : '');
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
if (!server || !isRunnable(server)) {
  console.error('Kotlin LSP server not found or not runnable. Set KOTLIN_LSP_SERVER (binary path)'
    + ' or KOTLIN_LSP_HOME (distribution root), or put intellij-server on PATH');
  process.exit(1);
}

function timeoutFromEnv(name, fallback) {
  const raw = Number(process.env[name]);
  return Number.isFinite(raw) && raw > 0 ? raw : fallback;
}
const readyTimeoutMs = timeoutFromEnv('KOTLIN_LSP_PLAN_READY_TIMEOUT_MS', 300000);
const callTimeoutMs = timeoutFromEnv('KOTLIN_LSP_CALL_TIMEOUT_MS', 180000);

// ---- op arguments ----------------------------------------------------------

let args;
if (op === 'format' || op === 'organize-imports') {
  if (!argv[1]) {
    console.error('usage: ' + op + ' <file>');
    process.exit(1);
  }
  args = { file: argv[1] };
}
for (const f of [args.file].filter(Boolean)) {
  if (!/\.(kt)$/i.test(f)) {
    console.error('not a Kotlin source file (.kt only; .kts build scripts have no model): ' + f);
    process.exit(1);
  }
  if (!fs.existsSync(f)) {
    console.error('file not found: ' + f);
    process.exit(1);
  }
}

const targetPaths = [...new Set([args.file].filter(Boolean))];

// ---- LSP plumbing (shared shape with kotlin-lsp-plan.js) -------------------

const fileUri = p => pathToFileURL(path.resolve(p)).href;
const projectUri = pathToFileURL(repoRoot).href;

const lockFile = path.join(repoRoot, '.kotlin', 'lsp-cache', process.platform + '.lock');
function liveLockHolderPid() {
  let pid;
  try { pid = parseInt(fs.readFileSync(lockFile, 'utf8'), 10); } catch { return 0; }
  if (!pid) return 0;
  try { process.kill(pid, 0); return pid; }
  catch (e) { return e.code === 'EPERM' ? pid : 0; }
}
fs.mkdirSync(path.dirname(lockFile), { recursive: true });
function acquireLock() {
  try {
    fs.writeFileSync(lockFile, String(process.pid), { flag: 'wx' });
    return;
  } catch (e) {
    if (e.code !== 'EEXIST') {
      console.error('cannot write the edit lock ' + lockFile + ': ' + e.message);
      process.exit(1);
    }
    const holder = liveLockHolderPid();
    if (holder) {
      console.error('another diagnostics/plan/edit run is already in progress (pid ' + holder
        + ', lock ' + lockFile + ')');
      process.exit(1);
    }
    try {
      fs.unlinkSync(lockFile);
      fs.writeFileSync(lockFile, String(process.pid), { flag: 'wx' });
    } catch (e) {
      console.error('cannot take over the stale lock ' + lockFile + ': ' + e.message);
      process.exit(1);
    }
  }
}
function releaseLock() {
  try { if (fs.readFileSync(lockFile, 'utf8') === String(process.pid)) fs.unlinkSync(lockFile); } catch { /* gone */ }
}
acquireLock();
process.on('exit', releaseLock);

let proc;
try {
  proc = spawn(server, ['--stdio', '--system-path', indexDir], {
    cwd: repoRoot,
    stdio: ['pipe', 'pipe', 'pipe'],
  });
} catch (e) {
  console.error('cannot start Kotlin LSP (' + e.code + '): ' + e.message);
  process.exit(1);
}

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    console.error('interrupted by ' + signal + ': stopping the Kotlin LSP process');
    proc.kill();
    process.exit(signal === 'SIGINT' ? 130 : 143);
  });
}

let buf = Buffer.alloc(0);
let nextId = 1;
const pending = new Map();
let exitInfo;
// The applyEdit the server sends for applyEdit-gated ops lands here; the reply
// is owned by this script, so the server never writes the files itself.
let capturedApplyEdit;
let capturedApplyEditResolve;
function markClosed(info) {
  if (exitInfo) return;
  exitInfo = info;
  for (const { reject, timer, method } of pending.values()) {
    clearTimeout(timer);
    reject(new Error(method + ': Kotlin LSP ' + info));
  }
  pending.clear();
}
proc.on('error', (e) => markClosed('errored with ' + e.code
  + (e.code === 'EACCES' ? ' (not runnable: check the execute bit, chmod +x)' : '')));
proc.stderr.on('data', () => { /* launcher noise */ });
proc.on('close', (code, signal) => markClosed('exited with code ' + code
  + (signal ? ' on signal ' + signal : '')));
proc.stdin.on('error', () => {});

function send(method, params, timeoutMs) {
  if (exitInfo) return Promise.reject(new Error('cannot send ' + method + ': Kotlin LSP already ' + exitInfo));
  const id = nextId++;
  const limit = timeoutMs || callTimeoutMs;
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, method, params }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
  return new Promise((resolve, reject) => {
    const t = setTimeout(() => {
      pending.delete(id);
      reject(new Error('timeout waiting for ' + method + ' after ' + limit + 'ms'));
    }, limit);
    pending.set(id, { resolve, reject, timer: t, method });
  });
}
function notify(method, params) {
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', method, params }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
}
function reply(id, result) {
  const body = Buffer.from(JSON.stringify({ jsonrpc: '2.0', id, result }), 'utf8');
  proc.stdin.write(`Content-Length: ${body.length}\r\n\r\n`, 'ascii');
  proc.stdin.write(body);
}

function handleMessage(msg) {
  if (msg.method !== undefined) {
    if (msg.id !== undefined) {
      if (msg.method === 'workspace/configuration') {
        // Same contract as kotlin-lsp-plan.js: the reply must be an array with
        // one entry per requested item; a bare null crashes consumers of the
        // list. Nothing here consumes configuration, so unknown sections get
        // an empty object (nothing enabled).
        const items = (msg.params && msg.params.items) || [];
        reply(msg.id, items.map(() => ({})));
        return;
      }
      if (msg.method === 'workspace/applyEdit') {
        capturedApplyEdit = (msg.params && msg.params.edit) || null;
        if (capturedApplyEditResolve) { capturedApplyEditResolve(capturedApplyEdit); capturedApplyEditResolve = undefined; }
        // Ownership is ours: the server must not write. The reply reports the
        // disk truth -- in dry mode the edit was captured but not applied to
        // the workspace, so applied:false is accurate, not a failure signal.
        // With --apply the write happens right after the op body returns.
        reply(msg.id, { applied: apply === true });
      } else {
        reply(msg.id, null);
      }
    }
    return;
  }
  const p = pending.get(msg.id);
  if (!p) return;
  pending.delete(msg.id);
  clearTimeout(p.timer);
  if (msg.error) { p.reject(new Error(p.method + ': Kotlin LSP error ' + msg.error.code + ' ' + msg.error.message)); return; }
  p.resolve(msg.result);
}

// Framing runs on bytes, not on a UTF-16 string: Content-Length counts bytes,
// and a UTF-16 length disagrees the moment a frame carries a non-ASCII char.
function extract() {
  for (;;) {
    const headerEnd = buf.indexOf('\r\n\r\n');
    if (headerEnd < 0) return;
    const header = buf.slice(0, headerEnd).toString('ascii');
    const m = /Content-Length: (\d+)/i.exec(header);
    if (!m) { buf = buf.slice(headerEnd + 4); continue; }
    const len = parseInt(m[1], 10);
    const start = headerEnd + 4;
    if (buf.length < start + len) return;
    const body = buf.slice(start, start + len).toString('utf8');
    buf = buf.slice(start + len);
    try { handleMessage(JSON.parse(body)); } catch (e) { console.error('bad frame: ' + e.message); }
  }
}
proc.stdout.on('data', d => { buf = Buffer.concat([buf, d]); extract(); });

// ---- readiness -------------------------------------------------------------

async function waitReady(uris) {
  const deadline = Date.now() + readyTimeoutMs;
  const open = new Set(uris);
  let lastErr;
  while (Date.now() < deadline && open.size > 0) {
    await new Promise(r => setTimeout(r, Math.min(5000, Math.max(0, deadline - Date.now()))));
    for (const uri of [...open]) {
      if (Date.now() >= deadline) break;
      const pollTimeout = Math.max(1, Math.min(deadline - Date.now(), callTimeoutMs));
      let symbols;
      try {
        symbols = await send('textDocument/documentSymbol', { textDocument: { uri } }, pollTimeout);
      } catch (e) {
        if (e.message.startsWith('cannot send')) throw e;
        lastErr = e; break;
      }
      if (Array.isArray(symbols) && symbols.length > 0) open.delete(uri);
    }
  }
  if (open.size > 0) {
    throw new Error('no project model for ' + open.size + ' document(s) within '
      + readyTimeoutMs + 'ms' + (lastErr ? '; last error: ' + lastErr.message : ''));
  }
}

// ---- edits: collect, print, apply ------------------------------------------

function collectEditGroups(we) {
  // WorkspaceEdit arrives in either shape: `changes` (uri -> TextEdit[]) or
  // `documentChanges` (TextDocumentEdit plus optional file operations). Both
  // are folded into per-file groups; file operations are carried alongside.
  const groups = new Map(); // path -> TextEdit[]
  const fileOps = [];
  if (we && we.changes) {
    for (const [uri, edits] of Object.entries(we.changes)) {
      const p = uriToPath(uri);
      if (!groups.has(p)) groups.set(p, []);
      groups.get(p).push(...edits);
    }
  }
  if (we && Array.isArray(we.documentChanges)) {
    for (const dc of we.documentChanges) {
      if (dc.textDocument && Array.isArray(dc.edits)) {
        const p = uriToPath(dc.textDocument.uri);
        if (!groups.has(p)) groups.set(p, []);
        groups.get(p).push(...dc.edits);
      } else if (dc.kind) {
        fileOps.push(dc);
      }
    }
  }
  return { groups, fileOps };
}

function uriToPath(uri) {
  let p;
  try { p = fileURLToPath(uri); } catch { return uri; }
  return p.replace(/^(.):/, m => m.toUpperCase());
}

function applyToText(text, edits) {
  // LSP positions are 0-based {line, character}; character counts UTF-16 units
  // within the line as the server sees it. Offsets are computed against the
  // raw text (CRLF stays inside the line span), and edits go in from the end
  // so earlier ranges stay valid.
  const lineStarts = [0];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '\n') lineStarts.push(i + 1);
  }
  const offsetOf = (pos) => {
    if (pos.line >= lineStarts.length) return text.length;
    // LSP asks clients to clamp an out-of-line character to the line's length.
    // The clamp bound is the line's logical length: on CRLF that stops before
    // the \r (clamping to the \n index would drop an insert between \r and \n
    // and split the line ending), on LF it stops before the \n.
    const nlIndex = pos.line + 1 < lineStarts.length ? lineStarts[pos.line + 1] - 1 : text.length;
    const lineEnd = nlIndex > lineStarts[pos.line] && text[nlIndex - 1] === '\r' ? nlIndex - 1 : nlIndex;
    return lineStarts[pos.line] + Math.min(pos.character, Math.max(0, lineEnd - lineStarts[pos.line]));
  };
  // Edits apply from the end so earlier offsets stay valid. When two edits
  // start at the same offset (LSP explicitly allows multiple insertions at one
  // position), the last-applied edit lands first in the text, so among equal
  // ranges the array order must be reversed to keep the server's order.
  const indexed = edits.map((e, i) => ({ e, i }));
  indexed.sort((a, b) => {
    const ab = offsetOf(b.e.range.start) - offsetOf(a.e.range.start);
    if (ab !== 0) return ab;
    const ae = offsetOf(b.e.range.end) - offsetOf(a.e.range.end);
    if (ae !== 0) return ae;
    return b.i - a.i;
  });
  const sorted = indexed.map(x => x.e);
  let out = text;
  for (const e of sorted) {
    const start = offsetOf(e.range.start);
    const end = offsetOf(e.range.end);
    out = out.slice(0, start) + (e.newText || '') + out.slice(end);
  }
  return out;
}

function printEditGroups(groups, fileOps) {
  if (groups.size === 0 && fileOps.length === 0) { console.log('(no edits)'); return; }
  for (const [p, edits] of groups) {
    console.log(p + ' edits=' + edits.length);
    for (const e of edits.slice(0, 12)) {
      const s = e.range.start, t = e.range.end;
      const snippet = (e.newText || '').replace(/\s*\n\s*/g, ' ').trim();
      console.log('  ' + (s.line + 1) + ':' + (s.character + 1) + ' -> ' + (t.line + 1) + ':'
        + (t.character + 1) + ' new="' + snippet.slice(0, 100) + '"');
    }
    if (edits.length > 12) console.log('  ... and ' + (edits.length - 12) + ' more');
  }
  for (const op of fileOps) console.log('file op: ' + op.kind + ' ' + JSON.stringify(op.uri || (op.oldUri + ' -> ' + op.newUri)));
}

function writeEdits(groups, fileOps) {
  for (const [p, edits] of groups) {
    const before = fs.readFileSync(p, 'utf8');
    const after = applyToText(before, edits);
    if (after !== before) fs.writeFileSync(p, after, 'utf8');
    console.log('applied: ' + p + ' edits=' + edits.length);
  }
  for (const op of fileOps) {
    // No wired op produces file operations, and an unexpected one must not
    // touch the disk: print and refuse, the caller decides by hand.
    console.log('skipped file op ' + op.kind + ' (this script does not apply file operations)');
  }
}

// ---- main ------------------------------------------------------------------

(async () => {
  try {
    const init = await send('initialize', {
      processId: process.pid,
      clientInfo: { name: 'kotlin-lsp-edit' },
      rootUri: projectUri,
      rootPath: repoRoot,
      workspaceFolders: [{ uri: projectUri, name: path.basename(repoRoot) }],
      capabilities: { window: { workDoneProgress: true }, textDocument: { diagnostic: {} } },
      initializationOptions: { indexDir, buildTools: { [projectUri]: 'gradle' } },
    });
    console.log('== Kotlin LSP ' + (init.serverInfo && init.serverInfo.version) + ' ==');
    notify('initialized', {});
    for (const p of targetPaths) {
      notify('textDocument/didOpen', {
        textDocument: { uri: fileUri(p), languageId: 'kotlin', version: 1, text: fs.readFileSync(p, 'utf8') },
      });
    }
    await waitReady(targetPaths.map(fileUri));

    let we;
    if (op === 'format') {
      console.log('== format ' + args.file + (apply ? ' [apply]' : ' [dry]') + ' ==');
      const edits = await send('textDocument/formatting', {
        textDocument: { uri: fileUri(args.file) },
        options: { tabSize: 4, insertSpaces: true },
      });
      we = { changes: { [fileUri(args.file)]: Array.isArray(edits) ? edits : [] } };
    } else if (op === 'organize-imports') {
      console.log('== organize-imports ' + args.file + (apply ? ' [apply]' : ' [dry]') + ' ==');
      const acts = await send('textDocument/codeAction', {
        textDocument: { uri: fileUri(args.file) },
        range: { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } },
        context: { diagnostics: [], only: ['source.organizeImports'] },
      });
      const list = Array.isArray(acts) ? acts : [];
      const action = list.find(a => a.kind === 'source.organizeImports');
      if (!action) {
        console.log('(no organize-imports action for this file)');
      } else if (action.edit) {
        we = action.edit;
      } else if (action.command) {
        // The applyEdit request usually arrives while executeCommand is still
        // pending, but the protocol does not promise the order: wait for the
        // capture explicitly instead of assuming the response frame came last.
        // The race only bounds the wait; the edit itself is read from the
        // capture variable afterwards, so both arrival orders yield it.
        capturedApplyEdit = undefined;
        let captureTimer;
        const captured = new Promise(resolve => {
          capturedApplyEditResolve = value => { clearTimeout(captureTimer); resolve(value); };
        });
        await send('workspace/executeCommand', { command: action.command.command, arguments: action.command.arguments });
        we = await Promise.race([
          captured,
          new Promise(resolve => { captureTimer = setTimeout(() => resolve(capturedApplyEdit), 10000); }),
        ]);
        // Clear both ends of the race no matter which side won: the timer is
        // assigned after the early-arrival order fires the resolve, so clearing
        // inside the resolve wrapper alone misses that order.
        clearTimeout(captureTimer);
        capturedApplyEditResolve = undefined;
        if (!we) console.log('(the command returned no applyEdit)');
      } else {
        console.log('(organize-imports action carries neither edit nor command)');
      }
    }

    const { groups, fileOps } = collectEditGroups(we || {});
    printEditGroups(groups, fileOps);
    if (apply) writeEdits(groups, fileOps);
    else if (groups.size > 0 || fileOps.length > 0) console.log('(dry run: nothing written; rerun with --apply)');

    // shutdown is a courtesy call with its own short budget: an unresponsive
    // server must not stretch the tail, and the explicit process.exit below is
    // what ends the run -- the server's JVM grandchild can inherit the stdio
    // pipe handles and outlive proc.kill (which only reaches the launcher), so
    // an open stdout pipe would otherwise hold this event loop open forever.
    try { await send('shutdown', null, 10000); } catch (e) { console.error('shutdown: ' + e.message); }
    notify('exit', {});
    if (exitInfo) process.exit(process.exitCode || 0);
    await new Promise(resolve => {
      const timer = setTimeout(() => { try { proc.kill(); } catch (e) {} resolve(); }, 10000);
      proc.once('close', () => { clearTimeout(timer); resolve(); });
    });
    process.exit(process.exitCode || 0);
  } catch (e) {
    console.error('FAILED: ' + e.message);
    try { proc.kill(); } catch (e2) {}
    // Same tail rule as the success path: a JVM grandchild holding the stdio
    // pipes would keep this event loop alive past the failure report.
    process.exit(1);
  }
})();
