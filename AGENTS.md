# AGENTS.md

Guidance for coding agents working in this repository. Read this before making changes.

## Project overview

Android app that surfaces low-level system characteristics of the device: Treble and GSI compatibility, Mainline/APEX modules, system-as-root, A/B partitions, security patch levels, and related build properties.

Stack: Kotlin, XML views with ViewBinding (not Compose), MVVM with unidirectional data flow (coroutines + StateFlow), kotlinx.serialization, Ktor for networking, libsu for shell access, JNI/NDK for binder protocol detection. No DI framework.

- Product introduction and downloads: [README.md](README.md)

## Build and verify

```bash
./gradlew assembleFossDebug      # what CI builds (GitHub Actions)
./gradlew testFossDebugUnitTest  # unit tests (template-only today)
./gradlew lintFossDebug          # Android lint
```

- On Windows, `gradlew.bat` works the same.
- Flavors (dimension `IssueTracker`): `Foss` is the default (no tracking, `-Foss` version name suffix); `Firebase` is the Play variant and requires `google-services.json`, which is gitignored.
- Debug builds work out of the box; release signing is configured outside the repository (see README).
- JDK 25 (Adoptium) is auto-provisioned via foojay (`gradle/gradle-daemon-jvm.properties`).
- `:binderDetector` native code needs NDK 30.0.16138531 and CMake 4.1.2 (the versions CI installs).
- Configuration cache and parallel builds are enabled; keep custom tasks configuration-cache compatible.

## Modules

- `:app` — the application. The namespace is also the applicationId; debug builds add a `.debug` suffix. Its `sourceSets` register the package-adjacent res directories (see Gotchas).
- `:base` — Android library with the `IProperty` / `IShell` abstractions and shared extensions.
- `:binderDetector` — Android library with C++ sources under `src/main/cpp`, exposed through JNI.
- `build-logic` — included build holding the convention plugins; the single source of SDK and build values for all modules.

## Architecture

Each feature lives in its own package under `ui` (`ui.home`, `ui.others`, `ui.prop`, `ui.settings`) with the same layering:

```
Fragment (ViewBinding) → ViewModel (StateFlow) → Repository → DataSource
```

- `MainActivity` hosts the four feature fragments with a `BottomNavigationView` (add/hide/show; fragment tag is the menu item id).
- `BaseListFragment` / `BaseListViewModel` drive every list page: `StateFlow<State<List<MyModel>>>` with `loadJob` dedup — don't reintroduce redundant loads on recreation.
- `State` is a sealed interface with `NotInitialized` / `Loading` / `Done` (there is no `Error` state yet).
- `MyAdapter` is a `ListAdapter` diffed by `MyModel.key`.
- ViewModels are created through `viewModelFactory` + `CreationExtras` (`MY_REPOSITORY_KEY`); fragments construct their repositories in `extrasProducer`.
- The bundled `lld.json` data is copied to the external files dir (`LldManager`) and refreshed online via Ktor when the user allows network.

## Adding a detection item

Current workflow (list order = call order):

1. Add property keys / shell commands to the feature `DataSource` (home items mostly go in `AndroidDataSource`).
2. Add a `detect…()` method to the feature `Repository` returning `MyModel`.
3. Call it from the feature `ViewModel.collectModels()` — the call order defines the list order.
4. Add the strings to the feature package's `strings.xml` (default English) plus the three translation files.
5. If the item needs a new package with resources, register its res directory in `app/build.gradle.kts` sourceSets.
6. Verify with `./gradlew assembleFossDebug`.

## Code rules

Rules for new code — they encode decisions from the architecture review; don't make existing debt worse:

- No static event buses: never put `SharedFlow`/`StateFlow` in a ViewModel companion object. Cross-feature data goes through a repository.
- Don't read `MyApplication.sharedPreferences` directly; route through the owning repository.
- Never write to the global `myAndroid` (`AndroidVersionExt`) — the `isAtLeast…()` helpers read it from everywhere.
- Blocking work (shell, system properties, files, network) runs on `Dispatchers.IO`, not `Dispatchers.Default`.
- Reuse one `HttpClient` per process; don't build one per request.
- `ShellDefault` is dead code — don't enable it. The shell implementation is `ShellLibSu` (libsu).

## Gotchas

Conventions that differ from a typical Android project:

- Version catalogs are split: use `libsAndroid`, `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty` (files in `gradle/toml/`). There is no default `libs` accessor.
- SDK, build-tools, and NDK versions live only in `gradle/toml/build.toml` (with an `isPreview` toggle) and reach modules through the `build-logic` convention plugins. Never hardcode SDK levels in module scripts.
- Resources live under `java/<package>/…/res` package paths (for example `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`); there is no `app/src/main/res`. New res directories must be registered in `app/build.gradle.kts` sourceSets.
- Kotlin 2.4 with the experimental compiler flags declared in `build-logic` — those flags are intentional, don't remove them. Code style is `official`.
- minSdk is 23: gate newer APIs with the `isAtLeastAndroidX()` helpers or `@RequiresApi`.

## Localization

- Strings are split per feature package. Supported locales: default (English), `zh-rCN`, `zh-rTW`, `fr-rFR` (`localeFilters` + `generateLocaleConfig`).
- New user-facing strings always need the default English entry; keep the three translation files in sync when you can.

## Git and CI

- PRs target `develop` (the default branch). Use semantic commit prefixes: `fix:`, `feat:`, `docs:`, `chore:`.
- CI runs on push/PR to `develop` and builds `assembleFossDebug` only.
- `master` carries `lld.json` data updates and is merged into `develop` periodically — don't open PRs against it.

## Documentation

- Developer docs live under `docs/dev/`.
- Chinese-language docs use `-cn` suffixed directories (for example `docs/dev/architecture-review-cn/`).
- Keep docs short; when one grows too long, split it by natural seams.

## Never commit

- Never commit or force-add credentials, signing material, or local configuration — the root `.gitignore` already excludes them.
- If a change seems to require committing such files, stop and ask first.
