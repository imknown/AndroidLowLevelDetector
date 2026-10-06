# AGENTS.md

Guidance for coding agents working in this repository. Read this before making changes.

Neither side is authoritative by default: **code and docs can both be wrong**. Code settles what the app does *today* — a verifiable fact. What the app *should* do belongs to the owner, and a doc's statement of intent can itself be a mistake (written too far, or invented). So classify a doc-vs-code disagreement before editing — stale doc, code bug, unimplemented requirement, or a doc that overstated the requirement — and when the evidence won't classify it, or the question is whether stated intent really is the owner's, ask.

Working principles:

- **Memory**: this file is the durable memory — settled decisions, conventions, gotchas, and doc corrections land here, so the next session starts from them instead of rediscovering them. Unsorted personal material goes in [docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md). When unsure what or where to record, discuss with the user.
- **Never guess**: investigate the codebase and docs first; base every change on evidence you can point to (file, line, doc section). When something cannot be determined or multiple valid approaches exist, present findings and ask the user; write `unknown` rather than inventing a value. A conflict is a stop sign, not a licence to pick: when the evidence does not settle which side is wrong (doc vs doc, doc vs code), show both sides and ask. Scope an instruction to what was named — a second sentence that looks like the same problem goes on a list you ask about, not into the same edit.
- **English by default**: anything you generate — docs, comments, commit messages — is in English unless the user specifies otherwise. Exception: code comments are written in the owner's chat language during implementation and translated to English at the commit gate (Task workflow, step 1). Whatever language a doc is written in, its punctuation is ASCII, spaced as in English (no `，。、：（）「」`): see [Documentation rules](#documentation-rules).
- **No sensitive information** in any document, memory included: no privacy data, passwords, keys, certificates, or signing material.

Conventions that deliberately diverge from the mainstream / official template are called out inline below — they are not smells to fix, so don't "normalize" them in passing. When a statement here disagrees with the code, trust the code and fix this file.

## Project overview

Android app that surfaces low-level system characteristics: Treble and GSI compatibility, Mainline/APEX modules, system-as-root, A/B partitions, Binder bitness, security patch levels. Stack: Kotlin, Jetpack Compose (single Activity, Navigation 3), MVVM + StateFlow unidirectional data flow, kotlinx.serialization, Ktor, libsu, JNI/NDK. DI by Metro (compile-time, no reflection).

- Application id `net.imknown.android.forefrontinfo`; version info in `gradle/toml/build.toml`.
- Modules: `:app` (Compose UI, features under `ui`) · `:base` (`IProperty`/`IShell` abstractions) · `:binderDetector` (C++ via JNI) · `build-logic` (convention plugins).
- Product introduction and download links are in the [root README](README.md).

## Build and verify

```bash
./gradlew assembleFossDebug      # what CI builds (GitHub Actions)
./gradlew testFossDebugUnitTest  # unit tests (template-only today)
./gradlew lintFossDebug          # Android lint
```

- **Check the LSP first**: before editing code in a language, check whether that language's LSP is configured in this environment; if it is not, help the user set one up first (e.g. the Kotlin LSP: the `kotlin-lsp` plugin hosts JetBrains ILS — launch its server with `--stdio`, wait for the `intellij/ready-for-test` notification, then pull `textDocument/diagnostic`; ILS never pushes diagnostics, and `textDocument/documentSymbol` doubles as an "is it really analyzing" check). After each edit batch, run LSP diagnostics — plus the Gradle tiers above — then have a fresh-context subagent review the uncommitted diff in the background, report the findings to the owner, and stop: nothing is committed without the owner's explicit say-so, spec flow or not. Inspection-level LSP findings (e.g. "Use destructuring declaration") are treated like syntax warnings — fixed directly, not documented. The Kotlin side is already wired here: `scripts/kotlin-lsp-diagnostics.js` runs the whole handshake in one command, resolving the ILS installation from `KOTLIN_LSP_SERVER`, `KOTLIN_LSP_HOME` (the conventional user-level environment variable pointing at the distribution root), or `intellij-server` on `PATH`. The index cache is the gitignored `.kotlin/lsp-cache` — disposable, and always launched with `--system-path` because a bare ILS launch picks a random temp dir and re-indexes from scratch; rebuild it with `<ILS distribution>/bin/warmup.py <repo> <repo>/.kotlin/lsp-cache --server <ILS distribution>/bin/intellij-server --build-tool gradle`.

## Build conventions

- Flavors (dimension `IssueTracker` — not the usual `mode` / `store`): `Foss` is the default (no tracking, `-Foss` version name suffix); `Firebase` is the Play variant and requires `google-services.json`, which is gitignored. `AndroidApplicationFirebaseConventionPlugin` attaches Firebase deps as `firebaseImplementation` and disables the GoogleServices / Crashlytics tasks for Foss, so a Foss build never needs that file.
- Debug builds work out of the box; debug adds an `applicationIdSuffix = ".debug"` so it installs side by side with release — remember the suffix when dealing with app identity (permissions, adb). Release signing is configured outside the repository, in gitignored `local.properties` (see README).
- JDK 25 (Adoptium) on two independent tracks: code compilation via `jvmToolchain`, the Gradle Daemon via `gradle/gradle-daemon-jvm.properties` (generated by `updateDaemonJvm`). Don't conflate the two. `./gradlew -q javaToolchains` to inspect.
- `:binderDetector` native code needs the NDK and CMake versions pinned in `gradle/toml/build.toml` (currently NDK 30.0.16248370, CMake 4.1.2) — the exact versions CI installs, so bump them and CI in lockstep.
- Version catalogs are split into five files (`gradle/toml/`: `build` / `android` / `kotlin` / `google` / `thirdParty`): use `libsAndroid`, `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty`. There is no default `libs` accessor. Dependencies and versions live exclusively in these catalogs; never write bare coordinates in a module `build.gradle.kts`, and decide which category a dependency belongs to before referencing it.
- Version tiers: an RC / Stable dependency or toolchain version may go into production directly. A Beta / Alpha / Canary one may too, but only when it has been researched thoroughly, its known issues can be fixed or avoided, and the adoption has been evaluated.
- SDK, build-tools, and NDK versions live only in `gradle/toml/build.toml` (with an `isPreview` toggle) and reach modules through the `build-logic` convention plugins. Never hardcode SDK levels in module scripts.
- Kotlin 2.4 with a set of experimental compiler flags declared in `build-logic`, grouped by the Kotlin version that introduced them — those flags are intentional, don't remove them; re-review the groups on every Kotlin upgrade and drop stabilized ones. Code style is `official`.
- Repositories are content-filtered (`google()` narrowed by `includeGroupByRegex`) with `FAIL_ON_PROJECT_REPOS`; `jitpack.io` exists only in the main build's dependency repositories.
- Configuration cache (with parallel + integrity checks) and parallel builds are enabled; keep custom tasks configuration-cache compatible.
- Build scans use the Develocity plugin but never publish (`publishing.onlyIf { false }`) — local scans only.

## Module details

- `:app` — the application; the namespace is also the applicationId. Its `sourceSets` register the package-adjacent res directories: resources live under `java/<package>/.../res` paths (for example `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`), there is no `app/src/main/res`, and new res directories must be registered in `app/build.gradle.kts` sourceSets.
- `build-logic` — included build holding the convention plugins; the single source of SDK and build values for all modules. Shared configuration (SDK, desugaring, Java toolchain, Kotlin compiler args, test dependencies) lives in `build-logic/convention/src/main/kotlin/.../android/`; modules only apply plugins. Adding a convention plugin: implement it under `build-logic/convention`, then register an alias in the `[plugins]` section of `gradle/toml/android.toml`.

## Architecture

Each feature lives in its own package under `ui` (`ui.home`, `ui.others`, `ui.prop`, `ui.settings`) with the same layering:

```
Screen (Compose) → ViewModel (StateFlow) → Repository → DataSource
```

- Navigation is **Navigation 3** (`androidx.navigation3`), not the mainstream Navigation 2: the API shape is `NavKey` + back stack + entryProvider (`ui/navigation/NavKeys.kt`). Don't think in Nav2 terms (`NavHost(route = ...)`).
- **DI by Metro** — compile-time DI, a deliberate deviation from the Hilt / Koin mainstream (the hand-written companion `Factory` / `viewModel(factory = ...)` era is retired). ViewModels are `@Inject` + `@ViewModelKey` + `@ContributesIntoMap(AppScope::class, binding<ViewModel>())` and resolve at the Navigation 3 entry via `metroViewModel<...>()`; Repositories and DataSources are plain `@Inject` constructor injection; leaf bindings live in the binding containers in `di/AppGraph.kt`, and the graph factory binds `MyApplication`. The Gradle plugin, runtime, and MetroX artifacts are version-locked to one `version.ref` in `gradle/toml/thirdParty.toml`; a Kotlin upgrade requires a matching Metro upgrade — check the official compatibility matrix first.
- Settings are owned by `SettingsStore` (`ui/settings/repository/SettingsStore.kt`): the single observable holder of every setting's key/value plus the write entry points, bound in the graph and injected into consumers — nothing outside it touches SharedPreferences directly (the graph's SharedPreferences binding exists to feed it).
- Testability comes from **interface-first design**, not a mocking framework: `:base` defines `IProperty` / `IShell` with default implementations (`PropertyDefault` / `ShellDefault`) and has no aggregation classes — the graph binds both (`ShellLibSu` contributes `IShell` via `@ContributesBinding`, `PropertyDefault` is `@Provides`-bound by `PropertyContainer`), and `PropertyReader` (`ui/common`) wraps `IProperty` with the shared placeholder fallback.
- `BaseListViewModel` drives every list page with two `StateFlow`s — `modelsStateFlow: StateFlow<List<MyModel>?>` (null = cold start; a refresh deliberately keeps the previous list so the UI never flashes empty) and `isLoadingStateFlow` — plus `loadJob` dedup: don't reintroduce redundant loads on recreation. `onModelsLoaded()` runs after each load lands, for reconciling state that changed mid-build.
- Compose stability annotations (`@Immutable` / `@Stable`) are deliberate; re-evaluate them whenever a state class changes (follow the pattern in the comment atop `HomeViewModel` / `BaseListViewModel`, which explains *why* the annotation is safe).
- The bundled `lld.json` data is copied to the external files dir (`LldManager`) and refreshed online via Ktor when the user allows network; the GitHub or Gitee URL is chosen by timezone.
- Command execution uses libsu in **non-root** mode (`ui/common/ShellLibSu.kt`, with `Shell.FLAG_NON_ROOT_SHELL`): there is no root layer.

## Adding a detection item

Current workflow (list order = call order):

1. Add property keys / shell commands to the feature `DataSource`.
2. Add a `detect...()` method to the feature `Repository` returning `MyModel` (new Repository / DataSource classes join the graph via `@Inject` constructor injection; a missing annotation fails the build with `[Metro/MissingBinding]`).
3. Call it from the feature `ViewModel.collectModels()` — the call order defines the list order.
4. Add the strings to the feature package's `strings.xml` (default English) plus the three translation files.
5. If the item needs a new package with resources, register its res directory in `app/build.gradle.kts` sourceSets.
6. Verify with `./gradlew assembleFossDebug`.

## Code rules

Rules for new code — they encode settled decisions; don't make existing debt worse:

- Spell `ViewModel` out in full — never abbreviate it to `VM`, in identifiers, comments, commit messages, or docs. `VM` is already the abbreviation of *virtual machine*, which this app detects as a subject in its own right (process/VM architecture, `getArchitecture`), so the short form is ambiguous even where the meaning is obvious from context. `UseCase` and `DataSource` are spelled out the same way — no `UC` / `DS`.
- No static event buses: never put `SharedFlow`/`StateFlow` in a ViewModel companion object. Cross-feature data goes through a repository.
- The UI layer (Compose / ViewModels) never try/catches business exceptions: repositories and data sources either handle failures themselves or return wrapper types, and a function without the `OrThrow` suffix is contracted not to throw. An exception reaching the UI layer is a bug to fix at its source, not something the UI absorbs.
- The global `myAndroid` (`AndroidVersionExt`) has exactly two writers: `initMyAndroid()` at startup (`MyApplication.onCreate`, from the runtime `Build.VERSION`) and the known-values override in `HomeRepository.detectAndroid()`. Never assign to it anywhere else — the `isAtLeast...()` helpers read it from everywhere.
- minSdk is 24: gate newer APIs with the `isAtLeastAndroidX()` helpers or `@RequiresApi`.
- New code uses the newest syntax and standard-library APIs the pinned versions allow: the newest Kotlin syntax and std-lib APIs the current Kotlin version supports, the newest platform APIs the current compileSdk offers, and the newest APIs the current dependency versions offer — never write to an older idiom than the toolchain allows.
- When the newest usable syntax or API is itself Beta / experimental, don't adopt it unilaterally — present it and ask the owner how to handle it. The experimental compiler flags already enabled in `build-logic` are the settled set; this rule covers new opt-ins.
- Blocking work (shell, system properties, files, network) runs on `Dispatchers.IO`, not `Dispatchers.Default`.
- `ShellDefault` has no callers on purpose: it is the kept-in-reserve native shell implementation that does not depend on libsu (the live one is `ShellLibSu`). Don't delete it as dead code; if it is ever enabled, fix its read-after-`waitFor()` pipe deadlock first.

## Localization

- Strings are split per feature package. Supported locales: default (English), `zh-rCN`, `zh-rTW`, `fr-rFR` (`localeFilters` + `generateLocaleConfig`); add the locale to `localeFilters` when introducing a new language.
- New user-facing strings always need the default English entry; keep the three translation files in sync when you can.
- Per-language typographic punctuation (a full-width colon in Chinese, the French pre-colon space, ...) applies to user-facing localized copy only. The ASCII-punctuation rule under [Documentation rules](#documentation-rules) still governs docs, code comments, and non-copy string resources (storage keys, URIs, technical values).

## Task workflow (docs/spec)

The owner = the developer. AI handles research/analysis/coding/testing/review; the owner owns goals/boundaries/judgment/commit. Every gate ends in an explicit owner nod — nothing is committed without the owner's explicit say-so (spec flow or not) and no next step starts without one.

- Create `docs/spec/<yyyy-MM-dd-HH-mm-ss-Z>-<english-title>[-cn]/` (host-clock timestamp, short English title). The trailing `-cn` marks non-English reports; files inside a suffixed directory keep their prescribed names WITHOUT a language suffix.
- Owner pre-write (before AI starts): the owner hand-writes 2-3 sentences at the top of `plan.md` — the problem, their own approach, the biggest risk they predict; unchanged until the task ends, compared against at the close. (Guards against anchoring (Tversky & Kahneman) and hindsight bias: pre-register your own judgment before seeing the AI's plan.)
- AI writes `plan.md` (task level): the subtask split — each subtask's scope, why it is its own unit (minimal, high-cohesion, independently compilable changes), its risk level (high = security/data/core logic/unfamiliar areas; low = mechanical/boilerplate), and unfamiliar-area tags; a to-learn list at the end. The report language is the owner's call per task and applies to the plan and every subtask report. (Guards against cognitive load (Sweller): small chunks cut extraneous load; risk tiering prevents gate fatigue.)
- Plan gate: before starting subtasks, a fresh-context subagent reviews `plan.md` in the background; report the findings and wait for the owner to confirm them. Once the owner confirms, commit `plan.md` automatically, generate ALL per-subtask modification plan reports (`subtask-01..NN`) in one pass, and have a fresh-context subagent review that batch too — same loop as the code reviews (v1, v2, ...; findings wait for the owner's confirmation; only confirmed items get fixed) — before entering the per-subtask loop.
- Progress ledger: the spec dir carries `progress.md`; the AI updates it at every gate — current position, per-subtask gate state, suspended items (stashes), deviations, next action. Any new session/agent/model resumes by reading AGENTS.md + the ledger first, then executing the ledger's next action; in-flight steps (e.g. a running background review) are not captured — re-run the step the ledger points at. The ledger rides the next commit. At session start (or whenever the owner says "continue"), scan `docs/spec/*/progress.md` for unfinished tasks and resume from the ledger — the owner needs to remember nothing but the word "continue"; if several tasks are in flight, list them and ask. (Guards against self-review blind spots: an independent context removes in-context anchoring.)
- Per-subtask loop, strictly in order:
  0. Start-of-subtask comparison (default path — under AI-driven development the owner's unfamiliarity is structural, not staged; no tiering by domain): the owner provides a goal sentence + a question list (may be empty); for replacement-type tasks add a coverage check (audit the report's changed / deliberately-not-changed list against the overall goal for gaps); the decomposition and ordering are AI's responsibility — the owner does not pre-generate structure. AI obligations: a 5-10 line concept primer per subtask, the report answers every question on the list, and a coverage checklist is attached. Amend a stale report visibly before start; begin only when the owner says start; the gate tempo is set by the owner. Hands-on implementation is NOT mandated inside the flow — the owner arranges learning outside it. (Guards against the ironies of automation (Bainbridge 1983): under AI-driven development the owner structurally cannot keep up with implementation, so learning lives in the audit — question list / coverage check / primers — with the learning scope narrowed to architecture concepts and review judgment, not line-level implementation.)
  1. Implement (code comments written in the owner's chat language, explaining *why*, at the codebase's density; at the commit gate all new comments are translated to English — the committed codebase stays English; comment punctuation follows the ASCII rule) and verify with LSP diagnostics plus the build; for high-risk logic in unfamiliar areas, the owner reproduces it without AI first, then compares; if the AI fails on the same problem twice, stop — the owner takes over or re-splits. (Guards against deskilling — the ironies of automation (Bainbridge 1983): procedural memory needs practice; and against endless retries.)
  2. A fresh-context subagent reviews the uncommitted diff in the background — v1, then v2, v3, ... after each fix round; later rounds may resume the same reviewer for delta verification (its evidence base is already checked — faster and cheaper), but go back to a fresh reviewer when fixes rewrite large parts of the diff, when the owner rejected most findings, when the reused context grows bloated, or for a final independent acceptance pass; prefer a different model for the reviewer; every finding must carry verifiable evidence (file:line); for low-risk subtasks the owner may read the diff personally instead. (Guards against automation bias (Parasuraman & Riley) and correlated same-model blind spots: the evidence requirement turns recognition into verification; resuming a reviewer adds a self-confirming tendency, countered by the evidence requirement and the fresh-reviewer triggers.)
  3. On findings: report them; the owner re-reviews and confirms what really needs fixing; fix only what that round requires. If anything was fixed, loop back to 2 with the next version; if the owner confirms nothing needs fixing, step 4 applies.
  4. When a round has no new substantive findings: the owner leaves one sentence of their own words when approving (why this can pass); low-risk subtasks may merge steps 4/5 into a single nod. (The testing effect (Roediger & Karpicke 2006) upgrades recognition into retrieval; counters the fluency illusion and the illusion of explanatory depth (Rozenblit & Keil 2002); gate fatigue.)
  5. Commit the subtask (Conventional Commits + trailer), then wait for another nod before starting the next subtask.
- Convergence guard: review rounds chase substance — subjective style preferences and premature-optimization suggestions do not force another round; if rounds keep churning without new findings, surface that to the owner instead of looping forever.
- Deviation ledger: skipping a step is allowed, but log one line (task / which step / why) into the spec dir's progress ledger. (Behavioral economics: allowed-but-logged beats forbidden; prevents both silent process decay and wholesale abandonment of the workflow.)
- Close-out: clear the to-learn list; one cross-cutting retrospective (which classes of problems the AI gets wrong repeatedly -> distilled into a review checklist). (Spacing effect + metacognitive calibration — the long-term counter to the fluency illusion.)
- Archiving: when a task is fully closed out (the ledger's next action is none), its whole spec dir moves under `docs/spec/archived/` unchanged and is never edited again — the move itself declares the dir a **record** (see [Documentation rules](#documentation-rules)); a ledger header still saying living is superseded by the location, not a drift to fix in place. The resume scan (`docs/spec/*/progress.md`) does not reach one level deeper, so archived tasks never resurface as unfinished.

## Git and CI

- PRs target `develop` (the default branch). Commit messages follow Conventional Commits with **lowercase type + scope**: `fix(home): ...`.
- End every commit message with a trailer naming the agent, the model, and the reasoning effort level (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...) that produced it, e.g. `Generated with ZCode (GLM-5.3, effort: xhigh)`. Name the level `effort:` — it is the reasoning-effort knob itself, not a verdict on the reasoning; commits from 2026-09-22 and earlier spell it `reasoning:`, leave those as they are. Never guess a value; when the agent, model, or effort cannot be determined, ask the user what to record rather than silently writing `unknown`.
- Name the agent from evidence, not from habit: how to find it is up to you, but show what it rests on and get the user's agreement before writing it. Edition-level names differ (`Qoder CN` and `Qoder`, `Trae CN` and `Trae` are different ADEs); do not invent host-form suffixes such as `IDE` / `CLI` unless the user asks for them.
- CI builds `assembleFossDebug` only. `master` carries `lld.json` data updates — don't open PRs against it.

## Never commit

- Never commit or force-add credentials, signing material, or local configuration (`local.properties`, `keys/release.jks`, `google-services.json`) — the root `.gitignore` already excludes them.
- If a change seems to require committing such files, stop and ask first.

## Documentation rules

The rules for writing anything here. This file and the conventions it carries are **living** — they describe the present, and drift is a bug to fix in place. [docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md) is **scratch**: unsorted material, not a source of rules. A note may declare itself **frozen** or **record**: it keeps the text it was written with, corrections and later findings included, so cite it for history but never back-fill today's code into it. Nothing is frozen by default; anything written new is living until it says otherwise, and a new note says which status it has — **living**, **frozen / record**, or **scratch**. One status is declared by location instead of by the file: a spec dir under `docs/spec/archived/` is a **record** — the archiving move is the declaration, the files are not edited to carry it, and a status header inside that still says living is superseded.

- Whatever a doc tracks gets an ID, and IDs are shared across docs — reference them by ID.
- Fix docs **in place**: when a statement turns out to be wrong, rewrite it and everything downstream that leaned on it, so the doc reads as current truth on its own. Do not leave the superseded sentence standing with a parenthetical or blockquote correction appended to it, and do not label a corrected sentence with the check that produced it — the fact belongs in the sentence, not in a note about who checked it and when.
- Ask before adjudicating a conflict: when two docs disagree, or a doc and the code disagree and the evidence does not settle which side is wrong, present both sides and let the owner rule — then rewrite only the statement that was ruled on. Corrections do not spread: another sentence that looks like the same problem goes on a list to ask about, not into the same edit.
- A doc states only what the owner said or the code shows. No term, detection item, or qualifier is added to make a row or a sentence look complete; a glossary row is scoped to vocabulary the doc itself uses, and a row whose last reference is gone is asked about rather than kept because the term is generally known.
- What stays in prose: substance and navigation — the scope or exception the sentence is drawing, cross-references by ID, code symbols. **Who ruled what, and when, is not prose**: dated rulings belong in `git log`, not as an "owner ruled, <date>" tag on every sentence. A doc that carries a version row names the version and points at `git log`; it does not accumulate a revision log.
- English is the default language for documents, and file names are English by default; a non-English document carries a language suffix (Chinese: `-cn`). Links point at the file that exists, and where a document has no counterpart, at the one that does. Any language may still be used inside a doc when there is a reason.
- **ASCII punctuation in every language**: a Chinese (or Japanese) doc uses English punctuation marks, never CJK ones — `,` `.` `;` `:` `!` `?` `(...)` `"..."` instead of `，。；：！（）「」《》`. The one-to-one map: `，、` → `,` · `。` → `.` · `；` → `;` · `：` → `:` · `（）` → `()` · `「」『』《》` → `"` (or `'` nested) · `……`/`…` → `...` · `——` → ` — `. Non-punctuation glyphs stay as they are: `—` `·` `→` `←`, box-drawing in diagrams, and status emoji.
  Spacing follows English, whatever the script on either side: one space after a mark that is followed by more text (`每个条目, 每次检测`), no space before `,` `.;:!?)` and none inside `(` `"`. Two extra cases: a `(` that follows an identifier is a call, so no space there (`collectModels()`); `:` between digits stays tight (`16:9`, `12:30`).
  This holds for every doc, including the **frozen**, **record**, and **scratch** ones — punctuation there is formatting, not the substance those statuses preserve. When you write new text, or rework a paragraph, convert what the edit touches along with it, and check the punctuation when translating between languages.
  A heading's punctuation and spacing decide the GitHub anchor its table of contents points at, so re-spacing a heading means rewriting every `](#...)` that resolves to it — in the same edit. Explicit `<a id="...">` anchors are stable text and never move.
- No document (memory docs included) may contain sensitive information: privacy data, passwords, keys, certificates.
- Keep a note short; when one grows too long, split it along natural seams, keeping an index page in front.
