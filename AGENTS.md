# AGENTS.md

Guidance for coding agents working in this repository. Read this before making changes.

Neither side is authoritative by default: **code and docs can both be wrong**.  
Code settles what the app does *today*, a verifiable fact. What the app *should* do belongs to the owner,  
and a doc's statement of intent can itself be a mistake (written too far, or invented).  
So classify a doc-vs-code disagreement before editing (stale doc, code bug, unimplemented requirement,  
or a doc that overstated the requirement) and when the evidence won't classify it,  
or the question is whether stated intent really is the owner's, ask.

Working principles:

- **Memory**: this file is the durable memory: settled decisions, conventions, gotchas,  
  and doc corrections land here, so the next session starts from them instead of rediscovering them.  
  Unsorted personal material goes in [docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md).  
  When unsure what or where to record, discuss with the user.
- **Never guess**: investigate the codebase and docs first;  
  base every change on evidence you can point to (file, line, doc section).  
  Saying a rule does not exist anywhere is the same kind of claim and needs the same evidence:  
  grep the whole repo for it first, not just the files already read.  
  When something cannot be determined or multiple valid approaches exist, present findings and ask the user;  
  write `unknown` rather than inventing a value. A conflict is a stop sign, not a licence to pick:  
  when the evidence does not settle which side is wrong (doc vs doc, doc vs code), show both sides and ask.  
  Scope an instruction to what was named:  
- **Report before editing**: an instruction to change the project  
  starts a report gate, not the edit. When a doc tracks the target, re-verify what it says  
  against the current code first (docs can be stale or wrong; see the preamble),  
  then present the owner a short report before any edit: the current behavior and what is missing or wrong in it,  
  the candidate fixes where more than one exists, and which one you recommend and why.  
  The owner picks the fix; the edit starts only on the nod.  
  A spec subtask's step-0 report is this gate; anything else owes the short report on its own.  
  This is the spec flow's gate discipline applied to every change, documented or not,  
  and it binds every session.
  a second sentence that looks like the same problem goes on a list you ask about, not into the same edit.
- **English by default**: anything you generate (docs, comments, commit messages)  
  is in English unless the user specifies otherwise. Exception:  
  code comments are written in the owner's chat language during implementation  
  and translated to English at the commit gate  
  (Task workflow, step 1). Whatever language a doc is written in, its punctuation is ASCII,  
  spaced as in English (no `，。、：（）「」`): see [Documentation rules](#documentation-rules).
- **No sensitive information** in any document, memory included: no privacy data, passwords, keys,  
  certificates, or signing material.

Conventions that deliberately diverge from the mainstream / official template are called out inline below:  
they are not smells to fix, so don't "normalize" them in passing.  
When a statement here disagrees with the code, trust the code and fix this file.

## Where the detail lives

This file is the index: it keeps what binds every session, plus the parts of a procedure that are easy to get  
wrong. The long form of an activity lives in `docs/agents/` and is read when that activity starts.

| Read | When |
|---|---|
| [docs/agents/verification.md](docs/agents/verification.md) | you rely on an `android studio analyze-file` result |
| [docs/agents/build-conventions.md](docs/agents/build-conventions.md) | you change Gradle, a version catalog, a toolchain version, a CMakeLists.txt, or CI |
| [docs/agents/detection-item.md](docs/agents/detection-item.md) | you add a detection item to a feature |
| [docs/agents/task-workflow.md](docs/agents/task-workflow.md) | you open a spec directory, start a subtask, or close a task |
| [docs/agents/doc-formatting.md](docs/agents/doc-formatting.md) | you write a doc, a code comment, a commit message, or a reply |

## Project overview

Android app that surfaces low-level system characteristics: Treble and GSI compatibility,  
Mainline/APEX modules, system-as-root, A/B partitions, Binder bitness, security patch levels. Stack: Kotlin,  
Jetpack Compose (single Activity, Navigation 3), MVVM + StateFlow unidirectional data flow,  
kotlinx.serialization, Ktor, libsu, JNI/NDK. DI by Metro (compile-time, no reflection).

- Application id `net.imknown.android.forefrontinfo`; version info in `gradle/toml/build.toml`.
- Modules: `:app` (Compose UI, features under `ui`) · `:base` (`IProperty`/`IShell` abstractions) ·  
  `:binderDetector` (C++ via JNI) · `build-logic` (convention plugins).
- Product introduction and download links are in the [root README](README.md).

## Build and verify

```bash
./gradlew assembleFossDebug      # what CI builds (GitHub Actions)
./gradlew testFossDebugUnitTest  # unit tests (template-only today)
./gradlew lintFossDebug          # Android lint
```

- Docs and comment-only edits skip the build tiers (no code changed).
- **Analyze with Android Studio**: after each edit batch, run `android studio analyze-file` for every  
  changed file first, then the Gradle tiers above, then have a fresh-context subagent review the uncommitted  
  diff in the background, report the findings to the owner, and stop: nothing is committed without the  
  owner's explicit say-so, spec flow or not.  
  The reviewer owes verifiable evidence (file:line) for every finding, and prefer a different model for the  
  reviewer than the one that wrote the change: those duties bind every session, not only a spec task.  
  Review is round-based (v1, v2, ... after each fix round); the delta-pass and fresh-reviewer policy in the  
  Review step of [docs/agents/task-workflow.md](docs/agents/task-workflow.md) binds every session too.
  - [docs/agents/verification.md](docs/agents/verification.md) is the full contract for that command:  
    read it before you rely on a result, and beware that some runs look like a pass and are not  
    (the trap list and how to read the printed blocks live there).

## Build conventions

[docs/agents/build-conventions.md](docs/agents/build-conventions.md) is the layout of the build: the Foss /  
Firebase flavors and the `IssueTracker` dimension, application identity (debug adds an `applicationIdSuffix`),  
where release signing lives, the two JDK tracks, the NDK and CMake pins and their CI lockstep, the five  
catalog files and their accessors, repository filtering, the Kotlin flags, configuration cache, and  
Develocity scans. Read it before changing Gradle, a version catalog, a toolchain version, or CI.  
These bind any build edit:

- Dependencies and versions live exclusively in the five catalogs under `gradle/toml/`:  
  there is no default `libs` accessor, so never write a bare coordinate in a module `build.gradle.kts`,  
  and decide which catalog a dependency belongs to before referencing it.
- SDK, build-tools, and NDK versions live only in `gradle/toml/build.toml` (with an `isPreview` toggle) and  
  reach modules through the `build-logic` convention plugins. Never hardcode SDK levels in module scripts.
- No version number in prose: whichever build file carries the value is that value's source of truth, so a doc,  
  a code comment, and a commit message name the file or the key behind it instead of restating the number. The  
  holders are the five catalogs under `gradle/toml/` (reached through the catalog accessors and through  
  `buildVersion("<key>")` in `build-logic`), `gradle/wrapper/gradle-wrapper.properties` for Gradle itself, and  
  `gradle/gradle-daemon-jvm.properties` for the Gradle Daemon's JDK: a `.kts` script or a `.properties` file  
  counts exactly as much as a `.toml` catalog does. The ban covers the values our own build files carry; a  
  version that belongs to the outside world (an upstream library's release line, an Android API level) stays  
  in the sentence when it is its substance, because no file in this repo is its source of truth. A restated  
  number goes stale the moment the file behind it is bumped, and a stale number inside this file is a bug the  
  next session inherits as truth.
- The Kotlin compiler runs with a set of experimental flags declared in `build-logic`: those flags are  
  intentional, don't remove them; re-review the flag groups on every Kotlin upgrade and drop stabilized ones.  
  Code style is `official`.
- Version tiers: an RC / Stable dependency or toolchain version may go into production directly.  
  A Beta / Alpha / Canary one may too, but only when it has been researched thoroughly,  
  its known issues can be fixed or avoided, and the adoption has been evaluated.
- Configuration cache (with parallel + integrity checks) and parallel builds are enabled;  
  keep custom tasks configuration-cache compatible.

## Module details

- `:app`: the application; the namespace is also the applicationId.  
  Its `sourceSets` register the package-adjacent res directories:  
  resources live under `java/<package>/.../res` paths  
  (for example `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`),  
  there is no `app/src/main/res`,  
  and new res directories must be registered in `app/build.gradle.kts` sourceSets.
- `build-logic`: included build holding the convention plugins;  
  the single source of SDK and build values for all modules.  
  Shared configuration (SDK, desugaring, Java toolchain, Kotlin compiler args, test dependencies) lives in  
  `build-logic/convention/src/main/kotlin/.../android/`; modules only apply plugins.  
  Adding a convention plugin: implement it under `build-logic/convention`,  
  then register an alias in the `[plugins]` section of `gradle/toml/android.toml`.

## Architecture

Each feature lives in its own package under `ui` (`ui.home`, `ui.others`, `ui.prop`,  
`ui.settings`) with the same layering:

```
Screen (Compose) → ViewModel (StateFlow) → Repository → DataSource
```

- Navigation is **Navigation 3** (`androidx.navigation3`), not the mainstream Navigation 2:  
  the API shape is `NavKey` + back stack + entryProvider (`ui/navigation/NavKeys.kt`).  
  Don't think in Nav2 terms (`NavHost(route = ...)`).
- **DI by Metro**: compile-time DI,  
  a deliberate deviation from the Hilt / Koin mainstream  
  (the hand-written companion `Factory` / `viewModel(factory = ...)` era is retired). ViewModels are `@Inject` +  
  `@ViewModelKey` + `@ContributesIntoMap(AppScope::class, binding<ViewModel>())`  
  and resolve at the Navigation 3 entry via  
  `metroViewModel<...>()`; Repositories and DataSources are plain `@Inject` constructor injection;  
  leaf bindings live in the binding containers in `di/AppGraph.kt`,  
  and the graph factory binds `MyApplication`. The Gradle plugin, runtime,  
  and MetroX artifacts are version-locked to one `version.ref` in `gradle/toml/thirdParty.toml`;  
  a Kotlin upgrade requires a matching Metro upgrade, so check the official compatibility matrix first.  
  Deferred injection sites use the function type `() -> T`, not Metro's `Provider<T>`:  
  Metro treats `() -> T` as a provider by default, so a declared `Provider<T>` is the discouraged sugar it  
  reports as `DESUGARED_PROVIDER_WARNING` (the check reads the declared type, not how the value is called).
- Settings are owned by `SettingsStore` (`ui/settings/repository/SettingsStore.kt`):  
  the single observable holder of every setting's key/value plus the write entry points,  
  bound in the graph and injected into consumers,  
  so nothing outside it touches SharedPreferences directly  
  (the graph's SharedPreferences binding exists to feed it).
- Testability comes from **interface-first design**, not a mocking framework:  
  `:base` defines `IProperty` / `IShell` with default implementations (`PropertyDefault` / `ShellDefault`)  
  and has no aggregation classes: the graph binds both  
  (`ShellLibSu` contributes `IShell` via `@ContributesBinding`,  
  `PropertyDefault` is `@Provides`-bound by `PropertyContainer`),  
  and `PropertyReader` (`ui/common`) wraps `IProperty` with the shared placeholder fallback.
- `BaseListViewModel` drives every list page with two `StateFlow`s:  
  `modelsStateFlow: StateFlow<List<MyModel>?>`  
  (null = cold start; a refresh deliberately keeps the previous list so the UI never flashes empty) and  
  `isLoadingStateFlow`, plus `loadJob` dedup: don't reintroduce redundant loads on recreation.  
  `onModelsLoaded()` runs after each load lands, for reconciling state that changed mid-build.
- Compose stability annotations (`@Immutable` / `@Stable`) are deliberate;  
  re-evaluate them whenever a state class changes  
  (follow the pattern in the comment atop `HomeViewModel` / `BaseListViewModel`,  
  which explains *why* the annotation is safe).
- The bundled `lld.json` data is copied to the external files dir (`LldFileStore`)  
  and refreshed online via Ktor when the user allows network; the GitHub or Gitee URL is chosen by timezone.
- Command execution uses libsu in **non-root** mode  
  (`ui/common/ShellLibSu.kt`, with `Shell.FLAG_NON_ROOT_SHELL`): there is no root layer.

## Adding a detection item

[docs/agents/detection-item.md](docs/agents/detection-item.md) is the workflow (list order = call order):  
read it before adding a detection item to a feature.

## Code rules

Rules for new code. They encode settled decisions; don't make existing debt worse:

- Spell `ViewModel` out in full: never abbreviate it to `VM`, in identifiers, comments, commit messages,  
  or docs. `VM` is already the abbreviation of *virtual machine*,  
  which this app detects as a subject in its own right (process/VM architecture, `getArchitecture`),  
  so the short form is ambiguous even where the meaning is obvious from context.  
  `UseCase` and `DataSource` are spelled out the same way: no `UC` / `DS`.
- No static event buses: never put `SharedFlow`/`StateFlow` in a ViewModel companion object.  
  Cross-feature data goes through a repository.
- The UI layer (Compose / ViewModels) never try/catches business exceptions:  
  repositories and data sources either handle failures themselves or return wrapper types,  
  and a function without the `OrThrow` suffix is contracted not to throw.  
  An exception reaching the UI layer is a bug to fix at its source, not something the UI absorbs.
- The global `myAndroid` (`AndroidVersionExt`) has exactly two writers:  
  `initMyAndroid()` at startup (`MyApplication.onCreate`, from the runtime `Build.VERSION`)  
  and the known-values override in `HomeRepository.detectAndroid()`.  
  Never assign to it anywhere else: the `isAtLeast...()` helpers read it from everywhere.
- minSdk comes from `gradle/toml/build.toml`: gate newer APIs with the `isAtLeastAndroidX()` helpers or  
  `@RequiresApi`.
- New code uses the newest syntax and standard-library APIs the pinned versions allow:  
  the newest Kotlin syntax and std-lib APIs the current Kotlin version supports,  
  the newest platform APIs the current compileSdk offers,  
  and the newest APIs the current dependency versions offer,  
  so never write to an older idiom than the toolchain allows.
- When the newest usable syntax or API is itself Beta / experimental, don't adopt it unilaterally:  
  present it and ask the owner how to handle it.  
  The experimental compiler flags already enabled in `build-logic` are the settled set;  
  this rule covers new opt-ins.
- Blocking work (shell, system properties, files, network) runs on `Dispatchers.IO`, not `Dispatchers.Default`.
- `ShellDefault` has no callers on purpose:  
  it is the kept-in-reserve native shell implementation that does not depend on libsu  
  (the live one is `ShellLibSu`). Don't delete it as dead code; if it is ever enabled,  
  fix its read-after-`waitFor()` pipe deadlock first.

## Localization

- Strings are split per feature package. Supported locales: default (English), `zh-rCN`, `zh-rTW`, `fr-rFR`  
  (`localeFilters` + `generateLocaleConfig`);  
  add the locale to `localeFilters` when introducing a new language.
- New user-facing strings always need the default English entry;  
  keep the three translation files in sync when you can.
- Per-language typographic punctuation (a full-width colon in Chinese, the French pre-colon space,  
  ...) applies to user-facing localized copy only.  
  The ASCII-punctuation rule under [Documentation rules](#documentation-rules) still governs docs,  
  code comments, and non-copy string resources (storage keys, URIs, technical values).

## Task workflow (docs/spec)

The owner = the developer. AI handles research/analysis/coding/testing/review;  
the owner owns goals/boundaries/judgment/commit. Every gate ends in an explicit owner nod:  
nothing is committed without the owner's explicit say-so (spec flow or not) and no next step starts without one.

[docs/agents/task-workflow.md](docs/agents/task-workflow.md) is the flow itself: the spec directory layout, the  
owner's pre-write block, what `plan.md` owes (the subtask split, why each unit stands alone, its risk tier, the  
to-learn list), the plan gate, the per-subtask steps 0..5 with their fresh-context review rounds, the  
convergence guard, the deviation ledger, close-out, and archiving. Read it before you create a spec directory,  
and re-read the step you are in before you act on it.

These bind every session, spec task or not:

- Each spec dir carries `progress.md`, the ledger; the AI updates it at every gate,  
  and it rides the next commit.
- At session start, or whenever the owner says "continue", scan `docs/spec/*/progress.md` for unfinished tasks  
  and resume from the ledger: the owner needs to remember nothing but that word. Confirm the resume with the  
  owner before acting on it, even when only one task is unfinished; when several are in flight, list them and  
  ask which.

## Git and CI

- PRs target `develop` (the default branch).  
  Commit messages follow Conventional Commits with **lowercase type + scope**: `fix(home): ...`.
- End every commit message with a trailer naming the agent, the model,  
  and the reasoning effort level (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...) that produced it,  
  e.g. `Generated with ZCode (GLM-5.3, effort: xhigh)`. Name the level `effort:`.  
  It is the reasoning-effort knob itself, not a verdict on the reasoning;  
  commits from 2026-09-22 and earlier spell it `reasoning:`, leave those as they are. Never guess a value;  
  when the agent, model, or effort cannot be determined, ask the user what to record.  
  The ban is on writing `unknown` on your own, not on the word itself:  
  once you have asked and the user confirms the value genuinely cannot be determined  
  (for example the effort level is not shown in Auto mode),  
  recording `unknown` is the correct, agreed-on entry, not a guess.
- Name the agent from evidence, not from habit: how to find it is up to you,  
  but show what it rests on and get the user's agreement before writing it.  
  Edition-level names differ  
  (`Qoder CN` and `Qoder`, `Trae CN` and `Trae` are different AI development environments (ADEs));  
  do not invent host-form suffixes such as `IDE` / `CLI` unless the user asks for them.
- CI builds `assembleFossDebug` only. `master` carries `lld.json` data updates, so don't open PRs against it.

## Never commit

- Never commit or force-add credentials, signing material,  
  or local configuration (`local.properties`, `keys/release.jks`, `google-services.json`):  
  the root `.gitignore` already excludes them.
- If a change seems to require committing such files, stop and ask first.

## Documentation rules

The rules for writing anything here. This file and the conventions it carries are **living**:  
they describe the present, and drift is a bug to fix in place.  
[docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md) is **scratch**: unsorted material, not a source of rules.  
A note may declare itself **frozen** or **record**: it keeps the text it was written with,  
corrections and later findings included, so cite it for history but never back-fill today's code into it.  
Nothing is frozen by default; anything written new is living until it says otherwise,  
and a new note says which status it has: **living**, **frozen / record**, or **scratch**.  
One status is declared by location instead of by the file:  
a spec dir under `docs/spec/archived/` is a **record**: the archiving move is the declaration,  
the files are not edited to carry it, and a status header inside that still says living is superseded.

- Whatever a doc tracks gets an ID, and IDs are shared across docs, so reference them by ID.
- Fix docs **in place**: when a statement turns out to be wrong,  
  rewrite it and everything downstream that leaned on it, so the doc reads as current truth on its own.  
  Do not leave the superseded sentence standing with a parenthetical or blockquote correction appended to it,  
  and do not label a corrected sentence with the check that produced it: the fact belongs in the sentence,  
  not in a note about who checked it and when.
- Ask before adjudicating a conflict: when two docs disagree,  
  or a doc and the code disagree and the evidence does not settle which side is wrong,  
  present both sides and let the owner rule, then rewrite only the statement that was ruled on.  
  Corrections do not spread: another sentence that looks like the same problem goes on a list to ask about,  
  not into the same edit.
- A doc states only what the owner said or the code shows. No term, detection item,  
  or qualifier is added to make a row or a sentence look complete;  
  a glossary row is scoped to vocabulary the doc itself uses,  
  and a row whose last reference is gone is asked about rather than kept because the term is generally known.
- **Plain language**: every phrase must parse on first reading: no self-invented shorthand,  
  no metaphorical labels for things the doc then leans on (calling a set of work items a "bucket"),  
  and no abbreviation the document has not introduced.  
  Reuse terms AGENTS.md or the document itself already defines;  
  a genuinely new term is defined where it first appears. The same standard applies to code comments,  
  and to chat replies to the owner.
- What stays in prose: substance and navigation: the scope or exception the sentence is drawing,  
  cross-references by ID, code symbols. **Who ruled what, and when, is not prose**:  
  dated rulings belong in `git log`, not as an "owner ruled, <date>" tag on every sentence.  
  A doc that carries a version row names the version and points at `git log`;  
  it does not accumulate a revision log.
- English is the default language for documents, and file names are English by default; a non-English  
  document carries a language suffix (Chinese: `-cn`), as `docs/dev/module-structure-cn.md` does. Where a  
  document has a translated twin, the English file is the one in force and keeps the canonical name; the  
  translation carries the suffix, does not mention its twin, does not call itself a translation, and adds no  
  back-reference: the suffix states the relationship. No translated twin stands in the repo now, so  
  `README.md` and this file are single-language files.  
  Links point at the file that exists, and where a document has no counterpart in the reader's language, at  
  the one that does. Where a directory already has a naming pattern, follow it.  
  Any language may still be used inside a doc when there is a reason.
- **ASCII punctuation in every language**, **no dash as punctuation**, and **hard-wrap long lines by meaning**  
  hold in every doc, code comment, and chat reply, the **frozen**, **record**, and **scratch** ones included  
  (punctuation and wrapping are formatting, not the substance those statuses preserve).  
  [docs/agents/doc-formatting.md](docs/agents/doc-formatting.md) is the machinery: the CJK-to-ASCII mapping  
  and spacing cases, the dash rewrites per relation and the structural `--` exemptions, and the table and  
  code-comment wrapping cases. Read it before you write a doc, a code comment, a commit message, or a reply  
  to the owner, and whenever you convert or reflow text that already exists.
- No document (memory docs included) may contain sensitive information:  
  privacy data, passwords, keys, certificates.
- Keep a note short; when one grows too long, split it along natural seams, keeping an index page in front.
