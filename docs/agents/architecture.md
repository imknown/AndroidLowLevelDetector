# Architecture Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Architecture section.  
> Read this before you touch navigation, the DI graph, a ViewModel or a state class, a list page,  
> settings persistence, or shell/property access.  
> The one-sentence decisions stay in AGENTS.md; this file carries the detail and the why.

Per-screen design is not a separate document: read the code under `ui/`; this file carries the  
cross-cutting decisions and their reasons.

## Navigation

Navigation is **Navigation 3** (`androidx.navigation3`), not the mainstream Navigation 2:  
the API shape is `NavKey` + back stack + entryProvider (`ui/navigation/NavKeys.kt`).  
Don't think in Nav2 terms (`NavHost(route = ...)`).

## Dependency injection

DI is by **Metro** (compile-time, no reflection), a deliberate deviation from the Hilt / Koin mainstream  
(the hand-written companion `Factory` / `viewModel(factory = ...)` era is retired). ViewModels are `@Inject` +  
`@ViewModelKey` + `@ContributesIntoMap(AppScope::class, binding<ViewModel>())`  
and resolve at the Navigation 3 entry via `metroViewModel<...>()`; Repositories and DataSources are plain  
`@Inject` constructor injection; leaf bindings live in the binding containers in `di/AppGraph.kt`,  
and the graph factory binds `MyApplication`. The Gradle plugin, runtime, and MetroX artifacts are  
version-locked to one `version.ref` in `gradle/toml/thirdParty.toml`; a Kotlin upgrade requires a matching  
Metro upgrade, so check the official compatibility matrix first.

Deferred injection sites use the function type `() -> T`, not Metro's `Provider<T>`:  
Metro treats `() -> T` as a provider by default, so a declared `Provider<T>` is the discouraged sugar it  
reports as `DESUGARED_PROVIDER_WARNING` (the check reads the declared type, not how the value is called).

## Settings ownership

Settings are owned by `SettingsStore` (`ui/settings/repository/SettingsStore.kt`):  
the single observable holder of every setting's key/value plus the write entry points,  
bound in the graph and injected into consumers, so nothing outside it touches SharedPreferences directly  
(the graph's SharedPreferences binding exists to feed it).

## Interface-first testability

Testability comes from **interface-first design**, not a mocking framework:  
`:base` defines `IProperty` / `IShell` with default implementations (`PropertyDefault` / `ShellDefault`)  
and has no aggregation classes: the graph binds both  
(`ShellLibSu` contributes `IShell` via `@ContributesBinding`,  
`PropertyDefault` is `@Provides`-bound by `PropertyContainer`),  
and `PropertyReader` (`ui/common`) wraps `IProperty` with the shared placeholder fallback.

## BaseListViewModel

`BaseListViewModel` drives every list page with two `StateFlow`s:  
`modelsStateFlow: StateFlow<PersistentList<MyModel>?>`  
(null = cold start; a refresh deliberately keeps the previous list so the UI never flashes empty) and  
`isLoadingStateFlow`, plus `loadJob` dedup: don't reintroduce redundant loads on recreation.  
The `PersistentList` is built once in `setModels()` at that boundary,  
and `updateModelDetail()` patches it with `PersistentList.replacingAt()` (`set()` is deprecated).  
A composable never converts the collection:  
a conversion in a composable body reruns `O(n)` on every recomposition of that scope.  
`onModelsLoaded()` runs after each load completes, for reconciling state that changed mid-build.

## Compose stability

Compose stability annotations (`@Immutable` / `@Stable`) are deliberate;  
re-evaluate them whenever a state class changes  
(follow the pattern in the comment atop `HomeViewModel` / `BaseListViewModel`,  
which explains *why* the annotation is safe).

## Lld.json data flow

The bundled `lld.json` data is copied to the external files dir (`LldFileStore`)  
and refreshed online via Ktor when the user allows network; the GitHub or Gitee URL is chosen by timezone.

## Shell execution

Command execution uses libsu in **non-root** mode  
(`ui/common/ShellLibSu.kt`, with `Shell.FLAG_NON_ROOT_SHELL`): there is no root layer.
