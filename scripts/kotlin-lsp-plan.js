// Drive the JetBrains Kotlin LSP (the IntelliJ Language Server distribution)
// over stdio for the PLANNING phase, before code is written:
// references, definition, hover, call hierarchy, type hierarchy,
// workspace symbols, document symbols, diagnostics.
//
// Usage (one server session for all queries, batched):
//   node scripts/kotlin-lsp-plan.js <query> [<query> ...]
//
// Query tokens, all colon-suffixed, parsed right-to-left so that Windows
// drive letters survive (D:\path\X.kt:50:7:references):
//   <file>:<line>:<col>:<method>   position query, line and col are 1-based
//   <file>:symbols                 document symbols of the file
//   <file>:diagnostics             diagnostics of the file, all severities
//   wsymbol:<query text>           workspace-wide symbol search
//
// Position query methods:
//   references      who uses this symbol (declaration excluded)
//   definition      where this symbol is defined
//   typedef         the type of this symbol
//   implementation  implementations of this symbol
//   hover           type and documentation at this position
//   calls-in        incoming call hierarchy (who calls it)
//   calls-out       outgoing call hierarchy (what it calls)
//   type-super      supertypes of this type
//   type-sub        subtypes of this type
//
// Output is one finding per line, path:line:col with 1-based numbers, so an
// agent can act on it without parsing JSON. A symbol workspace/symbol found
// inside a jar (and any URI the platform cannot turn into a file path) has no
// file path: that line prints the raw URI, then the position and the symbol
// name. Severity is printed per LSP
// level without folding in both channels: this one and kotlin-lsp-diagnostics.js
// print ERROR / WARNING / INFORMATION / HINT separately, and a value outside
// the protocol prints as UNKNOWN(n) instead of being guessed into a bucket.
// There is no Fatal or Critical severity in LSP or in the IntelliJ severities
// behind this server.
//
// Exit code: 1 for a failed run (server, handshake, unanalyzed document) and
// for any diagnostics query that returned at least one severity=1 item;
// 0 otherwise; 130 or 143 when interrupted by SIGINT/SIGTERM, like the
// diagnostics channel. This matches the verification contract in
// docs/agents-cn/10-分析细则.md: Warning and below are printed, not gated.
//
// Cross platform: Windows, Linux and macOS run the same code. The server
// binary is resolved per platform (intellij-server.exe only on win32), the
// index cache directory is per platform, and the POSIX execute bit is checked
// before spawn. The Kotlin LSP installation is resolved from, in order:
//   KOTLIN_LSP_SERVER  full path to intellij-server(.exe) (explicit override)
//   KOTLIN_LSP_HOME    distribution root; <root>/bin/intellij-server is used
//   PATH               the intellij-server binary found on PATH
//
// Timeouts (ms), env-overridable for slow machines. The ready budget is a
// variable of its own because the verification channel needs a much larger
// cold-index budget than the planning channel:
//   KOTLIN_LSP_PLAN_READY_TIMEOUT_MS  default 300000, index-ready polling budget
//   KOTLIN_LSP_CALL_TIMEOUT_MS        default 180000, per-request timeout
//                                     (shared with kotlin-lsp-diagnostics.js)
//
// The lock file is shared with scripts/kotlin-lsp-diagnostics.js: two server
// instances against one platform cache contend for the index, so the two
// channels are mutually exclusive. See that script for the locking rationale.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const { pathToFileURL, fileURLToPath } = require('url');

const METHODS = ['references', 'definition', 'typedef', 'implementation', 'hover',
  'calls-in', 'calls-out', 'type-super', 'type-sub'];
const SEVERITIES = { 1: 'ERROR', 2: 'WARNING', 3: 'INFORMATION', 4: 'HINT' };

if (process.argv.length < 3) {
  console.error('usage: node scripts/kotlin-lsp-plan.js <query> [<query> ...]');
  console.error('query: <file>:<line>:<col>:<method> | <file>:symbols | <file>:diagnostics | wsymbol:<text>');
  console.error('method: ' + METHODS.join(', '));
  process.exit(1);
}

const repoRoot = path.join(__dirname, '..');
// Per platform: warmup.py hands this directory to the server as --system-path,
// which only the launcher and the JetBrains Runtime (JBR) that wrote it can
// reuse; each platform therefore keeps its own cache.
const indexDir = path.join(repoRoot, '.kotlin', 'lsp-cache', process.platform);
if (!fs.existsSync(indexDir)) {
  console.log('WARN: no index cache at ' + indexDir + ' -- this run cold-indexes and may exceed'
    + ' the ready timeout; warm it with <distribution>/bin/warmup.py first');
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
if (!server || !isRunnable(server)) {
  console.error('Kotlin LSP server not found or not runnable. Set KOTLIN_LSP_SERVER (binary path)'
    + ' or KOTLIN_LSP_HOME (distribution root), or put intellij-server on PATH');
  process.exit(1);
}

// Number.isFinite and > 0: a NaN or zero from a bad env value must not disable
// the deadline (NaN comparisons are always false) or every request timeout.
function timeoutFromEnv(name, fallback) {
  const raw = Number(process.env[name]);
  return Number.isFinite(raw) && raw > 0 ? raw : fallback;
}
const readyTimeoutMs = timeoutFromEnv('KOTLIN_LSP_PLAN_READY_TIMEOUT_MS', 300000);
const callTimeoutMs = timeoutFromEnv('KOTLIN_LSP_CALL_TIMEOUT_MS', 180000);

// ---- query parsing ---------------------------------------------------------

function splitQuery(token) {
  // Cut three colon-separated fields off the right end; whatever is left is
  // the file path, which keeps Windows drive letters intact.
  const m = /^(.*):(\d+):(\d+):([a-z-]+)$/i.exec(token);
  if (!m) return null;
  return { file: m[1], line: parseInt(m[2], 10), col: parseInt(m[3], 10), method: m[4].toLowerCase() };
}

const queries = [];
for (const token of process.argv.slice(2)) {
  const pos = splitQuery(token);
  if (pos) {
    if (!METHODS.includes(pos.method)) {
      console.error('unknown method "' + pos.method + '" in "' + token + '"; methods: ' + METHODS.join(', '));
      process.exit(1);
    }
    // A 0 would send a negative position to the server; anything non-numeric
    // fails the regex earlier, so this only sees real integers.
    if (pos.line < 1 || pos.col < 1) {
      console.error('line and col are 1-based and must be >= 1: "' + token + '"');
      process.exit(1);
    }
    queries.push({ kind: 'position', ...pos });
    continue;
  }
  if (token.startsWith('wsymbol:')) {
    queries.push({ kind: 'wsymbol', text: token.slice('wsymbol:'.length) });
    continue;
  }
  const m = /^(.*):(symbols|diagnostics)$/i.exec(token);
  if (m) {
    queries.push({ kind: m[2].toLowerCase(), file: m[1] });
    continue;
  }
  console.error('cannot parse query "' + token + '"');
  process.exit(1);
}
if (queries.length === 0) {
  console.error('no queries given');
  process.exit(1);
}

// Position targets must be .kt: Kotlin LSP builds no model for .kts Gradle build
// scripts and answers nothing meaningful for them (see 10-分析细则.md).
for (const q of queries) {
  const file = q.file;
  if (file && !/\.(kt)$/i.test(file)) {
    console.error('not a Kotlin source file (.kt only; .kts build scripts have no model): ' + file);
    process.exit(1);
  }
}

const targetPaths = [...new Set(queries.filter(q => q.file).map(q => path.resolve(q.file)))];
for (const p of targetPaths) {
  if (!fs.existsSync(p)) {
    console.error('file not found: ' + p);
    process.exit(1);
  }
}

// A wsymbol-only batch has no file to poll for readiness; pick any .kt found
// in a directory whose path looks like a source root. Walk the whole repo for
// src/main/java|kotlin directories (skipping build trees and VCS/IDE dirs)
// that hold a .kt somewhere beneath them, the way the self-check's
// findSourceRoot does: hardcoding module names breaks when the module list
// changes, and build outputs must not be picked as sentinel sources.
function findSentinel() {
  const skip = new Set(['.git', '.gradle', '.idea', '.kotlin', 'build', 'node_modules']);
  const queue = [repoRoot];
  while (queue.length > 0) {
    const dir = queue.shift();
    let entries;
    try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      if (!e.isDirectory() || skip.has(e.name)) continue;
      const full = path.join(dir, e.name);
      if (/[\\/]src[\\/]main[\\/](java|kotlin)$/.test(full)) {
        const kt = firstKtIn(full);
        if (kt) return kt;
      } else {
        queue.push(full);
      }
    }
  }
  return undefined;
}
function firstKtIn(dir) {
  const stack = [dir];
  while (stack.length > 0) {
    const d = stack.pop();
    let entries;
    try { entries = fs.readdirSync(d, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      const full = path.join(d, e.name);
      if (e.isDirectory()) stack.push(full);
      else if (/\.kt$/i.test(e.name)) return full;
    }
  }
  return undefined;
}
const sentinelPath = targetPaths[0] || findSentinel();
if (!sentinelPath) {
  console.error('no .kt file found to open as the readiness sentinel');
  process.exit(1);
}

// ---- LSP plumbing ----------------------------------------------------------

const fileUri = p => pathToFileURL(p).href;
const projectUri = pathToFileURL(repoRoot).href;

// Two instances against one platform cache contend for the index, and the run
// that loses fails in a way that looks like a code bug. The lock is created
// atomically (flag wx) and taken over once when a killed run left it behind.
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
function acquireLock() {
  try {
    fs.writeFileSync(lockFile, String(process.pid), { flag: 'wx' });
    return;
  } catch (e) {
    if (e.code !== 'EEXIST') {
      console.error('cannot write the plan lock ' + lockFile + ': ' + e.message);
      process.exit(1);
    }
    const holder = liveLockHolderPid();
    if (holder) {
      console.error('another diagnostics/plan run is already in progress (pid ' + holder
        + ', lock ' + lockFile + ')');
      process.exit(1);
    }
    // A lock left behind by a run that was killed hard: take it over once.
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
  console.error('cannot start Kotlin LSP (' + e.code + '): ' + e.message);
  process.exit(1);
}

// Without a handler Node stops on SIGINT/SIGTERM with no cleanup: the server it
// already started keeps running and holds the index cache and the lock. The
// status follows the shell convention of 128 + signal number. Exit also drops
// the lock through the 'exit' handler.
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    console.error('interrupted by ' + signal + ': stopping the Kotlin LSP process');
    proc.kill();
    process.exit(signal === 'SIGINT' ? 130 : 143);
  });
}

let buf = '';
let nextId = 1;
const pending = new Map();
let exitInfo;
// An exited server has to fail the run at once instead of leaving every caller
// on its timeout: writes to its stdin are dropped silently (no throw, no
// 'error' event), so a dead pipe gives no other signal.
function markClosed(info) {
  if (exitInfo) return;
  exitInfo = info;
  for (const { reject, timer, method } of pending.values()) {
    clearTimeout(timer);
    reject(new Error(method + ': Kotlin LSP ' + info));
  }
  pending.clear();
}
// Node reports a failed spawn either synchronously or through this event, and
// can also raise it for a process that started fine, so it is handled as a
// loss of the server rather than as "never started".
proc.on('error', (e) => markClosed('errored with ' + e.code
  + (e.code === 'EACCES' ? ' (not runnable: check the execute bit, chmod +x)' : '')));
proc.stderr.on('data', () => { /* the launcher prints JVM/idea paths; not ours to read */ });
proc.on('close', (code, signal) => markClosed('exited with code ' + code
  + (signal ? ' on signal ' + signal : '')));
proc.stdin.on('error', () => {});

function send(method, params, timeoutMs) {
  // A request written after the server was already gone is silently dropped by
  // the pipe, so without this it would sit out its full timeout.
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
  // Dispatch on the JSON-RPC shape first: a message carrying a method is a
  // request or notification from the server, one carrying only an id is a
  // response to a call we made. The distribution's warmup.py answers
  // server -> client requests with result:null across the board; ignoring
  // these stalls the handshake.
  if (msg.method !== undefined) {
    if (msg.id !== undefined) reply(msg.id, null);
    return;
  }
  const p = pending.get(msg.id);
  if (!p) return;
  pending.delete(msg.id);
  clearTimeout(p.timer);
  // A JSON-RPC error is a failed call, not a value. The method name comes from
  // the pending entry: an error response carries no method field of its own.
  if (msg.error) { p.reject(new Error(p.method + ': Kotlin LSP error ' + msg.error.code + ' ' + msg.error.message)); return; }
  p.resolve(msg.result);
}

function extract() {
  for (;;) {
    const idx = buf.indexOf('\r\n\r\n');
    if (idx < 0) return;
    const header = buf.slice(0, idx);
    const m = /Content-Length: (\d+)/i.exec(header);
    if (!m) { const nl = buf.indexOf('\n'); if (nl < 0) return; buf = buf.slice(nl + 1); continue; }
    const len = parseInt(m[1], 10);
    const start = idx + 4;
    if (buf.length < start + len) return;
    const body = buf.slice(start, start + len);
    buf = buf.slice(start + len);
    try { handleMessage(JSON.parse(body)); } catch (e) { console.error('bad frame: ' + e.message); }
  }
}
proc.stdout.on('data', d => { buf += d.toString('utf8'); extract(); });

// ---- output helpers --------------------------------------------------------

// 1-based path:line:col, the same shape the editor status bar shows.
function site(uri, range) {
  // fileURLToPath keeps the leading slash on POSIX and the drive letter on
  // Windows; a URI that is not a file (workspace/symbol also returns jar:
  // entries from the compile classpath) prints as-is. The server normalizes
  // drive letters to lower case; print them upper case so every line of one
  // run shows the same path shape.
  let p;
  try { p = fileURLToPath(uri); } catch { p = uri; }
  p = p.replace(/^(.):/, m => m.toUpperCase());
  const line = range && range.start ? range.start.line + 1 : '?';
  const col = range && range.start ? range.start.character + 1 : '?';
  return p + ':' + line + ':' + col;
}

function oneLine(s) { return String(s).replace(/\s*\n\s*/g, ' ').trim(); }

// ---- readiness -------------------------------------------------------------

// intellij/ready-for-test does not reliably arrive when an opened document
// carries compile errors (2026-11-08: two probes hung waiting for it), so
// readiness is polled as "documentSymbol answers non-empty" instead.
async function waitReady(uris) {
  const deadline = Date.now() + readyTimeoutMs;
  const open = new Set(uris);
  let lastErr;
  while (Date.now() < deadline && open.size > 0) {
    // Cap the poll sleep at what is left of the budget, so the deadline
    // itself, not the fixed poll interval, decides when the wait ends.
    await new Promise(r => setTimeout(r, Math.min(5000, Math.max(0, deadline - Date.now()))));
    for (const uri of [...open]) {
      // A per-poll cap keeps every request inside the budget; checking the
      // deadline per document keeps a multi-document batch from adding up past
      // it, so the reported timeout is a hard ceiling, not one that slow polls
      // can overrun.
      if (Date.now() >= deadline) break;
      const pollTimeout = Math.max(1, Math.min(deadline - Date.now(), callTimeoutMs));
      let symbols;
      try {
        symbols = await send('textDocument/documentSymbol', { textDocument: { uri } }, pollTimeout);
      } catch (e) {
        // A dead server would otherwise be polled until the ready timeout
        if (e.message.startsWith('cannot send')) throw e;
        lastErr = e; break;
      }
      if (Array.isArray(symbols) && symbols.length > 0) open.delete(uri);
    }
  }
  if (open.size > 0) {
    throw new Error('no project model for ' + open.size + ' document(s) within '
      + readyTimeoutMs + 'ms' + (lastErr ? '; last error: ' + lastErr.message : '')
      + ' (path or URI not recognised, or the index cache is missing)');
  }
}

// ---- query execution -------------------------------------------------------

async function runPositionQuery(q) {
  const uri = fileUri(path.resolve(q.file));
  const position = { line: q.line - 1, character: q.col - 1 };
  const td = { textDocument: { uri }, position };
  console.log('== ' + q.method + ' ' + q.file + ':' + q.line + ':' + q.col + ' ==');
  switch (q.method) {
    case 'references': {
      const refs = await send('textDocument/references', { ...td, context: { includeDeclaration: false } });
      if (!Array.isArray(refs) || refs.length === 0) { console.log('(no references)'); break; }
      for (const r of refs) console.log(site(r.uri, r.range));
      break;
    }
    case 'definition': case 'typedef': case 'implementation': {
      const method = q.method === 'definition' ? 'textDocument/definition'
        : q.method === 'typedef' ? 'textDocument/typeDefinition'
        : 'textDocument/implementation';
      const defs = await send(method, td);
      const list = Array.isArray(defs) ? defs : defs ? [defs] : [];
      if (list.length === 0) { console.log('(none)'); break; }
      for (const d of list) console.log(site(d.uri || d.targetUri, d.range || d.targetSelectionRange || d.targetRange));
      break;
    }
    case 'hover': {
      const h = await send('textDocument/hover', td);
      if (!h || !h.contents) { console.log('(none)'); break; }
      const c = h.contents;
      const text = typeof c === 'string' ? c
        : Array.isArray(c) ? c.map(x => (typeof x === 'string' ? x : (x.value || ''))).join(' ')
        : (c.value || '');
      console.log(site(uri, h.range) + ' ' + oneLine(text));
      break;
    }
    case 'calls-in': case 'calls-out': {
      const items = await send('textDocument/prepareCallHierarchy', td);
      if (!Array.isArray(items) || items.length === 0) { console.log('(none)'); break; }
      for (const item of items) {
        const calls = await send(q.method === 'calls-in' ? 'callHierarchy/incomingCalls' : 'callHierarchy/outgoingCalls',
          { item });
        for (const c of calls || []) {
          const it = c.from || c.to;
          console.log(site(it.uri, it.range) + ' ' + it.name);
        }
      }
      break;
    }
    case 'type-super': case 'type-sub': {
      const items = await send('textDocument/prepareTypeHierarchy', td);
      if (!Array.isArray(items) || items.length === 0) { console.log('(none: no hierarchy item)'); break; }
      let printed = 0;
      for (const item of items) {
        const types = await send(q.method === 'type-super' ? 'typeHierarchy/supertypes' : 'typeHierarchy/subtypes',
          { item });
        for (const t of types || []) { console.log(site(t.uri, t.range) + ' kind=' + t.kind + ' ' + t.name); printed++; }
      }
      if (printed === 0) console.log('(none: the server returned an empty hierarchy for this symbol)');
      break;
    }
    default:
      throw new Error('unhandled method ' + q.method);
  }
}

async function runFileQuery(q) {
  const uri = fileUri(path.resolve(q.file));
  console.log('== ' + q.kind + ' ' + q.file + ' ==');
  if (q.kind === 'symbols') {
    const symbols = await send('textDocument/documentSymbol', { textDocument: { uri } });
    (function walk(list, indent) {
      for (const s of list || []) {
        console.log(indent + site(uri, s.selectionRange) + ' kind=' + s.kind + ' ' + s.name);
        if (s.children) walk(s.children, indent + '  ');
      }
    })(symbols, '');
    return;
  }
  const diag = await send('textDocument/diagnostic', { textDocument: { uri } });
  const items = (diag && diag.items) || [];
  if (items.length === 0) console.log('CLEAN (0 items)');
  const counts = { 1: 0, 2: 0, 3: 0, 4: 0, UNKNOWN: 0 };
  for (const d of items) {
    const label = SEVERITIES[d.severity] || ('UNKNOWN(' + d.severity + ')');
    if (counts[d.severity] === undefined) counts.UNKNOWN++; else counts[d.severity]++;
    console.log('[' + label + '] ' + site(uri, d.range) + ' ' + (d.code || '') + ' ' + oneLine(d.message));
  }
  if (items.length > 0) {
    console.log('== ' + counts[1] + ' ERROR, ' + counts[2] + ' WARNING, '
      + counts[3] + ' INFORMATION, ' + counts[4] + ' HINT'
      + (counts.UNKNOWN ? ', ' + counts.UNKNOWN + ' UNKNOWN' : '') + ' ==');
  }
  if (counts[1] > 0) process.exitCode = 1;
}

async function runWsymbolQuery(q) {
  console.log('== wsymbol ' + q.text + ' ==');
  const symbols = await send('workspace/symbol', { query: q.text });
  if (!Array.isArray(symbols) || symbols.length === 0) { console.log('(none)'); return; }
  for (const s of symbols) console.log(site(s.location ? s.location.uri : s.uri, s.location ? s.location.range : s.range) + ' kind=' + s.kind + ' ' + s.name);
}

// ---- main ------------------------------------------------------------------

(async () => {
  try {
    const init = await send('initialize', {
      processId: process.pid,
      clientInfo: { name: 'kotlin-lsp-plan' },
      rootUri: projectUri,
      rootPath: repoRoot,
      workspaceFolders: [{ uri: projectUri, name: path.basename(repoRoot) }],
      capabilities: { window: { workDoneProgress: true }, textDocument: { diagnostic: {} } },
      // Same map form as the distribution's warmup.py: it pins the workspace
      // importer to Gradle rather than leaving the choice to the server
      // default, matching how the index cache here is warmed.
      initializationOptions: { indexDir, buildTools: { [projectUri]: 'gradle' } },
    });
    console.log('== Kotlin LSP ' + (init.serverInfo && init.serverInfo.version) + ' ==');
    notify('initialized', {});
    const toOpen = [...new Set([sentinelPath, ...targetPaths])];
    for (const p of toOpen) {
      notify('textDocument/didOpen', {
        textDocument: { uri: fileUri(p), languageId: 'kotlin', version: 1, text: fs.readFileSync(p, 'utf8') },
      });
    }
    await waitReady(toOpen.map(fileUri));

    for (const q of queries) {
      if (q.kind === 'position') await runPositionQuery(q);
      else if (q.kind === 'wsymbol') await runWsymbolQuery(q);
      else await runFileQuery(q);
    }

    try { await send('shutdown', null); } catch (e) { console.error('shutdown: ' + e.message); }
    notify('exit', {});
    // A server that already died has nothing left to flush and its close event
    // has already fired, so the graceful wait below would only burn its full
    // deadline; the exit status is already set, go straight out.
    if (exitInfo) return;
    // Graceful exit with a deadline rather than a fixed kill: a kill landing
    // while the JVM flushes its cache locks it for the next run.
    await new Promise(resolve => {
      const timer = setTimeout(() => { try { proc.kill(); } catch (e) {} resolve(); }, 10000);
      proc.once('close', () => { clearTimeout(timer); resolve(); });
    });
  } catch (e) {
    console.error('FAILED: ' + e.message);
    try { proc.kill(); } catch (e2) {}
    process.exitCode = 1;
  }
})();
