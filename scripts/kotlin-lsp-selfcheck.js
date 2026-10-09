// End-to-end self-check that the Kotlin LSP warmup actually worked, i.e. that the
// Gradle sync succeeded and the analyzer can really see the project model.
//
// Why this exists: a failed Gradle sync does not make ILS error out. It degrades
// to an incomplete model and still answers diagnostics, so a plain run on a clean
// file prints CLEAN and looks like success while the analyzer is blind. Checking
// `"tool":"gradle","status":"SUCCESS"` in the log is one signal; this is the
// stronger, end-to-end one: feed the analyzer a file known to contain problems
// and require those problems to come back. If they do, the model is live.
//
// The sentinel is temporary on purpose (owner's call): it is written into a
// source root just for this check and deleted in a finally, so nothing lands in
// git or in the build. It carries one warning-level finding (a non-lowercase
// function name) so it never breaks a build that might compile it; that same
// inspection warning vanishes when the sync fails, which is what makes it a valid
// probe.
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const repoRoot = path.join(__dirname, '..');
const diagnosticsScript = path.join(__dirname, 'kotlin-lsp-diagnostics.js');

// The sentinel has to live inside a source root ILS actually models: a file
// outside one yields no model, so documentSymbol returns 0 and the run cannot
// tell a blind analyzer from a clean file. Rather than hardcode one project's
// package, find a real source root in this repo. Android puts Kotlin under
// src/main/java as often as src/main/kotlin, so accept both. Requiring a .kt
// already present means the directory is a source root the build registers, not
// an empty tree ILS might skip.
function findSourceRoot(root) {
  const srcRoots = [];
  const skip = new Set(['.git', '.gradle', '.idea', '.kotlin', 'build', 'node_modules']);
  // Breadth-first so a shallow module's source root is picked before a deeply
  // nested one, keeping the choice stable and predictable.
  const queue = [root];
  while (queue.length > 0) {
    const dir = queue.shift();
    let entries;
    try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      if (!e.isDirectory() || skip.has(e.name)) continue;
      const full = path.join(dir, e.name);
      if (/[\\/]src[\\/]main[\\/](java|kotlin)$/.test(full)) {
        srcRoots.push(full);
      } else {
        queue.push(full);
      }
    }
  }
  // Keep only the roots that actually hold Kotlin source somewhere beneath them.
  for (const srcRoot of srcRoots) {
    if (hasKotlinFile(srcRoot)) return srcRoot;
  }
  return undefined;
}

function hasKotlinFile(dir) {
  const stack = [dir];
  while (stack.length > 0) {
    const d = stack.pop();
    let entries;
    try { entries = fs.readdirSync(d, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      const full = path.join(d, e.name);
      if (e.isDirectory()) stack.push(full);
      else if (/\.kt$/i.test(e.name)) return true;
    }
  }
  return false;
}

const sourceRoot = findSourceRoot(repoRoot);
if (!sourceRoot) {
  console.error('SELF-CHECK FAILED: no Kotlin source root (a src/main/java or'
    + ' src/main/kotlin directory containing a .kt file) found under ' + repoRoot
    + '; the sentinel can only be modelled inside one');
  process.exit(1);
}

// Written at the source root's top level with no package declaration, so it does
// not depend on any one project's package layout.
const sentinelName = 'ZzLspSelfCheckSentinel.kt';
const sentinelPath = path.join(sourceRoot, sentinelName);

// Deliberately triggers a warning a live analyzer reports and a blind
// (failed-sync) analyzer does not: FunctionName, a function named in PascalCase
// rather than lowercase. It is not a compile error, so a build that happens to
// compile this file is not broken by it.
const sentinelSource = `fun LspSelfCheckProbe() {
    val unused = 1
}
`;

function cleanup() {
  try { fs.unlinkSync(sentinelPath); } catch { /* already gone */ }
}

function main() {
  if (fs.existsSync(sentinelPath)) {
    console.error('sentinel already exists at ' + sentinelPath + ' (a previous run left it behind); removing it');
    cleanup();
  }
  fs.writeFileSync(sentinelPath, sentinelSource, 'utf8');

  const res = spawnSync(process.execPath, [diagnosticsScript, sentinelPath], {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
    maxBuffer: 32 * 1024 * 1024,
  });
  const out = (res.stdout || '') + (res.stderr || '');
  process.stdout.write(res.stdout || '');
  process.stderr.write(res.stderr || '');

  if (res.status !== 0) {
    throw new Error('diagnostics run on the sentinel exited ' + res.status
      + ': the warmup/sync did not produce a usable model');
  }
  // The whole point: a live analyzer reports the sentinel's planted warnings.
  // A CLEAN result means the sync silently failed and the model is empty.
  if (/CLEAN \(0 items\)/.test(out)) {
    throw new Error('sentinel reported CLEAN: the Gradle sync silently failed, so diagnostics are NOT trustworthy.'
      + ' The build JVM running Gradle is likely older than the project Java toolchain (build.toml javaToolchain);'
      + ' re-warm with JAVA_HOME pointing at a JDK of at least that version, then retry.');
  }
  const warnMatch = /==\s*(\d+)\s*ERROR,\s*(\d+)\s*WARN,\s*(\d+)\s*INFO\s*==/.exec(out);
  if (!warnMatch || parseInt(warnMatch[2], 10) < 1) {
    throw new Error('sentinel did not produce the expected warnings; the analyzer may not be seeing the project model');
  }
  console.log('== self-check PASSED: analyzer reported the sentinel warnings, so the Gradle sync is live ==');
}

try {
  main();
  cleanup();
  process.exit(0);
} catch (e) {
  cleanup();
  console.error('SELF-CHECK FAILED: ' + e.message);
  process.exit(1);
}
