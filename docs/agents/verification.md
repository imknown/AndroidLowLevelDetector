# Android Studio Analysis Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Build and verify section.  
> Read this before you rely on an `android studio analyze-file` result.  
> The invariant that stays in AGENTS.md is the duty itself: run it for every changed file after each edit batch.

`android studio analyze-file` drives a *running* Android Studio instance and reports its live inspection  
results for one file at a time: compiler errors, IDE inspections at every severity  
(Warning / WeakWarning / Info; that spectrum covers what the JetBrains IntelliJ Language Server reports  
and goes beyond it), Android Lint, and the inspections of installed IDE plugins.

## Usage

```bash
android studio analyze-file --project=AndroidLowLevelDetector <path>
```

- The path is relative to the current directory or absolute; the command takes one file per run.
- Add `--pid=<pid>` when more than one Android Studio instance is running.
- Anything Android Studio can analyze is in scope, including what its installed plugins inspect.

## Prerequisites

- The command carries no analyzer of its own: it queries a running Android Studio, so the project has to be  
  open there first.
- `android studio check` listing the project as `READY` is what makes a result worth reading.  
  When no instance has the project open, the run fails, so ask the owner to open Android Studio and  
  this project.

## Place in the verify flow

Run analyze-file first for every changed file, then `node scripts/ils-diagnostics.js <file>` for every
changed `.kt` file, then the Gradle tiers from AGENTS.md's Build and verify section
(`assembleFossDebug`, `testFossDebugUnitTest`, `lintFossDebug`): the per-file IDE result comes back in
seconds and catches compile errors and inspection findings early, the ILS then gives the complete list of
the compiler findings of those files, and the Gradle build runs its check over the whole batch. The
fresh-context diff review comes after all three.

The order is not arbitrary. analyze-file stays first because it answers in seconds and reads the live
IDE state, so the obvious break shows up before anything slower runs. The ILS step comes second because
it lists every site of a compiler finding of one file, which the analyze-file result does not do, and
because it needs a Gradle import of its own, so it costs more than the IDE check. The Gradle tiers
come last because they repeat the compiler findings over the whole batch, which is what they are for.

The Gradle tiers do not substitute for analyze-file, and the two only partly overlap:  
the Gradle tiers check that the batch compiles, plus compiler warnings and Android Lint,  
and they sometimes surface syntax-level warnings of their own;  
analyze-file adds the rest of the IDE inspection spectrum  
(usage findings at every severity, installed-plugin inspections), which a green build never sees.  
That is why the analyze-file duty holds for every changed file, resource files included:  
skipping it is a real coverage gap, not a redundant second pass.

The Gradle tiers answer the same class of findings as the JetBrains IntelliJ Language Server  
(the compiler ones) over the whole batch, one `Problem found:` block per finding, each with a `Location:`  
line, and that is where the complete list of compiler findings comes from. The ILS is the per-file channel  
for them: `node scripts/ils-diagnostics.js <file>` lists every site of every compiler and semantic finding  
of one file, which the analyze-file result cannot do (see the truncation below).

## Reading the result

- The exit code is not a verdict: a run that prints `ERROR` items still exits 0, and exit 1 only says the  
  call failed (no instance has the project open, or the file does not exist).
- Read the printed `ERROR` / `WARNING` / `INFO` blocks, each of which carries a line and a column.
- Inspection-level findings (for example "Use destructuring declaration") are treated like syntax warnings:  
  fixed directly, not documented.

## Why a result can be incomplete

- **One item per message text per file.** When two findings carry the same message text, only the first
  comes back, so N findings are not N sites: fixing the reported one uncovers the next.
  The rule is about the message text, not about the inspection: three `Property "alpha" / "beta" / "gamma"
  is never used` findings all come back, because each carries a different name, while four
  `Unnecessary non-null assertion (!!)` findings come back as one, because all four carry the same text.
  It applies to compiler errors as well: a file with two `Unresolved reference 'missingSymbolOne'.` items
  reports one of them.
  Measured over the whole repository: every Kotlin file (74 of them), 245 diagnostics, and no file ever
  held two items with the same text. The truncation is the IDE's, not the CLI's: the CLI only prints the
  list the IDE hands it. A file open in an editor comes back the same as a closed one, so opening it is
  not the cause. Count the sites in the source, not in the run.
- **An unindexed file prints a false `No issues found!`** (still exit 0). A file created in this session stays
  invisible to Android Studio until it indexes the file, so re-run the check after indexing has caught up before
  treating the silence as a pass. A path outside the `--project` named is invisible the same way.
- **A file Android Studio already knows is analyzed right away** once you edit it, so no waiting is needed
  there.

## The other channel: compiler and semantic findings per site

`android studio analyze-file` reports the IDE inspections, Android Lint and the installed plugin checks;
its truncation above applies to all of them. The JetBrains IntelliJ Language Server (ILS) is the second
channel: it reports the compiler and semantic findings of a file, every site of each, and it never
truncates. The two channels overlap on the compiler findings and differ on the inspections: the ILS
reports a narrower set of inspection findings (a `Function name 'ProbeOne' should start with a lowercase
letter` item comes back from it, while `Unused import directive` and the unused-declaration findings do
not), and `analyze-file` reports the full inspection spectrum.

Two scripts drive the ILS, both under `scripts/`:

```bash
node scripts/ils-diagnostics.js <path-to-kotlin-file>   # the findings of one file
node scripts/ils-selfcheck.js                            # is the analyzer really seeing the project
```

- **The project root comes from the script's own location**, so both scripts have to stay one level under
  the repository root; `scripts/` is where the repository keeps them. A copy run from outside that
  one-level location analyses a file outside the project model and answers `CLEAN (0 items)`.
- **The server is resolved from the environment**: `KOTLIN_LSP_SERVER` (the full path to
  `intellij-server`), `KOTLIN_LSP_HOME` (the distribution root; `<root>/bin/intellij-server` is used),
  or `intellij-server` on `PATH`.
- **The index cache lives at `<repo>/.kotlin/lsp-cache/<process.platform>`** (gitignored). It is
  per platform, because each platform runs its own distribution. Warm it with
  `<distribution>/bin/warmup.py <repo> <repo>/.kotlin/lsp-cache/<process.platform> --build-tool gradle`;
  a cold index takes minutes and can outrun the ready timeout, a warm one answers in under a minute.
- **The Gradle sync of the ILS runs under the JDK `JAVA_HOME` points at**, which has to be at least the
  project Java toolchain (`javaToolchain` in `gradle/toml/build.toml`). The `:build-logic:convention`
  included build is compiled to that toolchain's bytecode, so an older build JVM cannot load the
  convention plugins: the import fails with `Dependency requires at least JVM runtime version ...`, the
  ILS degrades to an incomplete model and still answers diagnostics, so the run prints a false `CLEAN`.
  Point `JAVA_HOME` at a JDK of at least that version before warming or diagnosing; the ILS distribution
  ships a matching JetBrains Runtime (JBR) under `<distribution>/jbr`.
  A build started from the command line is a different case: `./gradlew` takes its daemon JVM from
  `gradle/gradle-daemon-jvm.properties`, which pins it to the toolchain version and lets Gradle provision
  it, so `JAVA_HOME` does not decide the JVM there.
- **The self-check is what tells an analyzer without a project model from a clean file**: it writes a
  throwaway sentinel carrying one known warning into a source root, requires that warning to come back,
  and removes the sentinel afterwards. A `CLEAN` result on the sentinel means the sync failed, not that
  the code is fine.
- **The exit code** is 0 only when the pull succeeded and no ERROR item came back, 1 for a failed run
  or any ERROR item, and 130 or 143 when interrupted. The exit code does not check the sync, so pair it
  with the self-check before trusting a `CLEAN`.
- **The check covers `.kt` only**: the ILS builds no model for a `.kts` Gradle build script, so the
  script fails on one instead of printing an empty `CLEAN`. Verify build scripts with the Gradle build.
- `ILS_READY_TIMEOUT_MS` (default 1800000) and `ILS_CALL_TIMEOUT_MS` (default 180000) override the two
  waits on a slower machine.

