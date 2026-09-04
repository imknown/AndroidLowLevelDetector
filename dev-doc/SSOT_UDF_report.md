# SSOT / UDF Issue Analysis Report

> Analysis subject: `AndroidLowLevelDetector` (develop branch)
>
> Analysis scope: all Kotlin sources of the `app` and `base` modules
>
> Summary: the project is formally a Fragment → ViewModel (StateFlow) → Repository → DataSource
> UDF architecture, but it contains **17** substantive breakage points. Two root causes
> (unobservable settings and global mutable singletons) account for most of the issues, and there
> is 1 reproducible real bug (the list permanently stuck in Loading).

**Issue count by category: SSOT 7 / UDF 8 / Other 2**

| ID | Type | Location | One-line description | Severity |
|---|---|---|---|---|
| S1 | SSOT | 4 scattered sites | Settings are not observable; every layer reads SharedPreferences directly | 🔴 High (root cause) |
| S2 | SSOT | MainViewModel | `lastId` stored twice: in-memory + SavedStateHandle | 🟡 Medium |
| S3 | SSOT | AndroidVersionExt / HomeRepository | `myAndroid` is a global mutable singleton with two writers | 🔴 High |
| S4 | SSOT | HomeViewModel / LldManager / SettingsRepository | lld data has no single owner; cache writes are scattered | 🟡 Medium |
| S5 | SSOT | HomeRepository | `mounts` lazy cache diverges from its source | 🟢 Low |
| S6 | SSOT | HomeRepository.detectWebView | Two similarly-spelled variables; the legacy path reads the wrong one | 🟢 Low (Android 5 only) |
| S7 | SSOT | Factories of HomeViewModel / OthersViewModel / PropViewModel / SettingsViewModel | Dual-track state restoration: StateFlow already handles it, yet the injected SavedStateHandle is never read or written | 🟢 Low |
| U1 | UDF | SettingsViewModel (companion) | Static SharedFlow event bus + View-layer relay | 🔴 High (root-cause channel) |
| U2 | UDF | BaseListViewModel / HomeViewModel | List state is patched by index instead of derived | 🔴 High |
| U3 | UDF | BaseListFragment / SettingsFragment | View layer reads the data layer directly (prefs / theme) | 🟡 Medium |
| U4 | UDF | BaseListFragment / BaseListViewModel | Loading-coroutine lifecycle mismatch: unguarded concurrency + **stuck-in-Loading bug** | 🔴 High (actual bug) |
| U5 | UDF | SettingsViewModel / HomeRepository | Query functions with side effects (counter, global writes) | 🟢 Low |
| U6 | UDF | BaseListFragment / SettingsFragment | Fragments reach across layers to read the Activity's private binding | 🟢 Low |
| U7 | UDF | SettingsFragment / SettingsViewModel | `version` loading has no idempotency guard | 🟢 Low |
| U8 | UDF | MyAdapter / MyModel / BaseListViewModel | Split identity contract: the Adapter matches by `key`, patches match by index | 🟢 Low (latent) |
| O1 | Other | MyModelExt / MyAdapter / StateExt | Display snapshots frozen into state; title/detail of the same row resolved at different times; `State` has no Error variant | 🟡 Medium (user-visible) |
| O2 | Other | ShellManager / PropertyManager / MyApplication | `lateinit var instance` global mutable service locators | 🟢 Low |

### Category Criteria (verifiable)

| Type | Criteria | Coverage |
|---|---|---|
| **SSOT** (Single Source of Truth) | The same fact exists in **two or more** stores or read/write channels that can evolve independently and diverge | S1–S7: settings values / navigation ID / Android version / lld data / mount snapshot / built-in WebView version / list restoration state |
| **UDF** (Unidirectional Data Flow) | State **may only flow from upstream to downstream**, and state changes must be **pure derivations**; no reverse "event → patch", no cross-layer direct reads, no side-effecting queries, no lifecycle mismatches | U1–U8: event bus instead of state, patches instead of derivation, View reading the data layer directly, unguarded loading, side-effecting queries, cross-layer binding access, missing idempotency guard, split identity contract |
| **Other** | Directly related to state purity / UDF but not a typical SSOT/UDF breakage; or infrastructure problems that do not affect the state flow | O1 (frozen state / no Error variant), O2 (global service locator) |

> Key distinction: SSOT asks "how many owners does the same state have"; UDF asks "which direction
> state flows in, and whether it converges predictably". The two overlap (e.g., S1's unobservable
> settings are precisely what necessitates U1's event bus); this report classifies each issue by
> its "dominant lesion" and cross-references the overlaps.

---

## 1. Directory Structure and Issue Distribution

```
AndroidLowLevelDetector/
├── app/src/main/java/net/imknown/android/forefrontinfo/
│   ├── base/
│   │   └── MyApplication.kt ... S1 U3 O2
│   └── ui/
│       ├── MainActivity.kt ... U6
│       ├── MainViewModel.kt ... S2
│       ├── base/
│       │   ├── BaseFragment.kt ... —
│       │   ├── BaseViewModel.kt ... —
│       │   ├── ext/
│       │   │   ├── ToastExt.kt ... —
│       │   │   └── ViewBindingExt.kt ... —
│       │   └── list/
│       │       ├── BaseListFragment.kt ... S1 U1 U3 U4 U6
│       │       ├── BaseListViewModel.kt ... U2 U4 U8
│       │       ├── MyAdapter.kt ... U8 O1
│       │       ├── MyItemDecoration.kt ... —
│       │       ├── MyModel.kt ... U8
│       │       ├── MyModelExt.kt ... O1
│       │       └── MyViewHolder.kt ... —
│       ├── common/
│       │   ├── AndroidVersionExt.kt ... S3
│       │   ├── JsonExt.kt ... —
│       │   ├── LldManager.kt ... S4
│       │   ├── PropertyExt.kt ... —
│       │   ├── ShellExt.kt ... —
│       │   ├── ShellLibSu.kt ... —
│       │   ├── StateExt.kt ... O1
│       │   └── ViewExt.kt ... U1 (consumer side)
│       ├── home/
│       │   ├── HomeFragment.kt ... U1
│       │   ├── HomeViewModel.kt ... S1 S4 S7 U1 U2 U8
│       │   ├── model/ (Lld.kt, BaseInfo.kt) ... —
│       │   ├── repository/HomeRepository.kt ... S1 S3 S5 S6 O1
│       │   └── datasource/ (AndroidDataSource / LldDataSource / MountDataSource) ... S4 (involved)
│       ├── others/
│       │   ├── OthersFragment.kt ... —
│       │   ├── OthersViewModel.kt ... S3 (affected) S7
│       │   ├── repository/OthersRepository.kt ... S3 (affected)
│       │   └── datasource/ (6 files) ... —
│       ├── prop/
│       │   ├── PropFragment.kt ... —
│       │   ├── PropViewModel.kt ... S7
│       │   ├── repository/PropRepository.kt ... —
│       │   └── datasource/ (PropertiesDataSource / SettingsDataSource) ... —
│       └── settings/
│           ├── SettingsFragment.kt ... U1 U3 U5 U6 U7
│           ├── SettingsViewModel.kt ... S7 U1 U5 U7
│           ├── repository/SettingsRepository.kt ... S4
│           └── datasource/ (AppInfoDataSource / FingerprintDataSource) ... —
├── base/src/main/java/net/imknown/android/forefrontinfo/base/
│   ├── extension/ (CollectionExt / DateTimeExt / ExceptionExt) ... —
│   ├── property/
│   │   ├── PropertyManager.kt ... O2
│   │   └── impl/PropertyDefault.kt ... —
│   └── shell/
│       ├── ShellManager.kt ... O2
│       └── impl/ShellDefault.kt ... —
├── binderDetector/ (NDK library, stateless) ... —
└── build-logic/ (build convention plugins, unrelated to runtime) ... —
```

> "—" means the file has no SSOT/UDF issues. The most issue-dense files are `HomeRepository.kt`
> (5 issues) and `BaseListFragment.kt` (5 issues).

---

## 2. SSOT Issue Details

### S1 🔴 Settings are not observable; every layer reads SharedPreferences directly (root cause)

The persistence layer for settings is SharedPreferences with no observable wrapper. The same state
is patched together via "pull from everywhere + event notifications", spanning 4 architectural
layers:

```kotlin
// ① Application layer — base/MyApplication.kt:86 (reads theme at startup)
val themesValue = sharedPreferences.getString(
    getMyString(R.string.interface_themes_key), null
) ?: getMyString(R.string.interface_themes_follow_system_value)

// ② View layer — ui/base/list/BaseListFragment.kt:42-43 (reads initial scroll bar mode)
val scrollBarModeKey = MyApplication.getMyString(R.string.interface_scroll_bar_key)
val scrollBarMode = MyApplication.sharedPreferences.getString(scrollBarModeKey, null)
binding.recyclerView.setScrollBarMode(scrollBarMode)

// ③ ViewModel layer — ui/home/HomeViewModel.kt:45 (reads the network toggle)
val allowNetwork = MyApplication.sharedPreferences.getBoolean(
    MyApplication.getMyString(R.string.function_allow_network_data_key), false
)

// ④ Repository layer — ui/home/repository/HomeRepository.kt:1129 (reads the ordering toggle)
val shouldOrderByPackageNameFirst = MyApplication.sharedPreferences.getBoolean(
    MyApplication.getMyString(R.string.function_outdated_target_order_by_package_name_first_key),
    false
)
```

**Consequence:** "scrollBarMode" exists in multiple in-memory copies at runtime (one per
RecyclerView); the initial value comes from a direct read, subsequent updates come from a static
event stream (U1), and the two channels jointly determine the current value with no mechanism
guaranteeing convergence.

**Refactoring plan:**

```kotlin
// New file ui/settings/SettingsStore.kt — the single observable source of settings
class SettingsStore(context: Context) {
    private val Context.dataStore by preferencesDataStore("settings")
    private object Keys {
        val THEME = stringPreferencesKey("interface_themes")
        val SCROLL_BAR_MODE = stringPreferencesKey("interface_scroll_bar")
        val ALLOW_NETWORK = booleanPreferencesKey("function_allow_network")
        val OUTDATED_ORDER_BY_PACKAGE = booleanPreferencesKey("outdated_order_by_package")
    }

    val theme: Flow<String>            = context.dataStore.data.map { it[Keys.THEME] ?: FOLLOW_SYSTEM }
    val scrollBarMode: Flow<String?>   = context.dataStore.data.map { it[Keys.SCROLL_BAR_MODE] }
    val allowNetwork: Flow<Boolean>    = context.dataStore.data.map { it[Keys.ALLOW_NETWORK] ?: false }
    val outdatedOrderByPackage: Flow<Boolean> = context.dataStore.data.map { it[Keys.OUTDATED_ORDER_BY_PACKAGE] ?: false }
}
```

- Each ViewModel injects and `collect`s/`combine`s the corresponding Flows; delete all 4 direct
  reads;
- All of ①②③④ above switch to reading from `SettingsStore` (① the theme is still handed to
  `AppCompatDelegate` after being collected in the Application, but the read source is single);
- **Once this item is done, U1's two static SharedFlows, U2's event patches, and U3's direct prefs
  reads lose their reason to exist and can be removed together.**

---

### S2 🟡 `MainViewModel.lastId` is stored twice

```kotlin
// ui/MainViewModel.kt:14-23
@IdRes
var lastId = getSavedStateLastId()          // Copy 1: in-memory, public and mutable, can be modified directly bypassing the setter

fun setSavedStateLastId(@IdRes id: Int) {
    lastId = id                             // writes both copies in sync
    savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id   // Copy 2: SavedStateHandle
}

@IdRes
private fun getSavedStateLastId() = savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID]
    ?: R.id.navigation_home
```

**Problem:** the two copies can diverge; `MainActivity.switch()` reads the in-memory copy
([MainActivity.kt:92](../app/src/main/java/net/imknown/android/forefrontinfo/ui/MainActivity.kt#L92)).

**Refactoring plan:**

```kotlin
class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val lastId: StateFlow<Int> = savedStateHandle.getStateFlow(
        SAVED_STATE_HANDLE_KEY_LAST_ID, R.id.navigation_home
    )

    fun setLastId(@IdRes id: Int) {
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id
    }
}
// MainActivity: val lastId = mainViewModel.lastId.value
```

`savedStateHandle.getStateFlow` is itself an SSOT with restoration capability; the in-memory `var`
is entirely redundant.

---

### S3 🔴 `myAndroid` is a global mutable singleton with two writers

```kotlin
// ui/common/AndroidVersionExt.kt:62-64 — a global mutable object
class MyAndroid(var api: Int, var apiFull: String, var version: String, var dessert: String?)
val myAndroid = MyAndroid(Build.VERSION.SDK_INT, "${Build.VERSION.SDK_INT}.$minor", Build.VERSION.RELEASE)

// Writer ① — initMyAndroid(), called from MyApplication.onCreate() (AndroidVersionExt.kt:104-109)
myAndroid.api = api
myAndroid.apiFull = apiFull
myAndroid.version = version
myAndroid.dessert = dessert

// Writer ② — HomeRepository.detectAndroid() (HomeRepository.kt:107-114), overwrites the global during "detection"
if (android != null) {
    with(android) {
        myAndroid.api = api.toInt()
        myAndroid.apiFull = apiFull
        myAndroid.version = version
        myAndroid.dessert = name
    }
}

// Readers — all version-check functions (AndroidVersionExt.kt:114-122), widely used by Others/Home/Repositories
private val sdkInt get() = myAndroid.api
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
```

**Consequence:** "what Android version this device runs" depends on whether the Home page has run
detect yet. Concrete touchpoint: the outdated-APK filter threshold
`it.targetSdkVersion < myAndroid.api`
([HomeRepository.kt:1108](../app/src/main/java/net/imknown/android/forefrontinfo/ui/home/repository/HomeRepository.kt#L1108))
— reordering the calls to `detectAndroid` and `getOutdatedTargetSdkVersionApkModel` inside
`detect()` (line 139 vs line 160) changes the filter result, with no mechanism guaranteeing
consistency.

**Refactoring plan:**

```kotlin
// 1) Make it an immutable value
data class MyAndroid(val api: Int, val apiFull: String, val version: String, val dessert: String?)

val myAndroid: MyAndroid by lazy { initMyAndroid() }   // Single writer: computed once at app startup

// 2) detectAndroid's corrected result stays a local value used for rendering; it never writes back to the global
fun detectAndroid(lld: Lld?): MyModel {
    val runtime = myAndroid                                  // immutable snapshot
    val corrected = lld?.android?.known?.find { it.apiFull == runtime.apiFull }
        ?.let { runtime.copy(version = it.version, dessert = it.name) }
        ?: runtime
    // everything below uses corrected …
}
```

`isAtLeastAndroidX()` would read from the immutable `myAndroid`, making behavior independent of
timing.

---

### S4 🟡 lld data has no single owner; cache writes are scattered

lld.json has three sources (assets / local cache file / network), but "which one is currently in
effect" has no single owner:

```kotlin
// ① The ViewModel writes the data-layer cache file directly — ui/home/HomeViewModel.kt:76-83
val errorMessage = try {
    withContext(Dispatchers.IO) {
        LldManager.saveLldJsonFileOrThrow(lldString)     // the ViewModel writes a file!
    }
    null
} catch (e: Exception) { ... }

// ② The fallback/copy/version-comparison logic itself also writes files — ui/common/LldManager.kt:27
fun copyJsonIfNeededOrThrow() { ... }

// ③ SettingsRepository independently reads the assets version again — ui/settings/repository/SettingsRepository.kt:26-27
val assetLldVersion = withContext(Dispatchers.IO) {
    LldManager.getAssetLldVersion(MyApplication.instance.assets) ...
}

// ④ The fallback chain is orchestrated in the ViewModel — HomeViewModel.kt:57-126 (network fails → read file → read assets)
```

**Refactoring plan:** consolidate into a single entry point:

```kotlin
class LldRepository(
    private val lldDataSource: LldDataSource,
    private val settingsStore: SettingsStore,       // from S1
) {
    /** Single entry point: handles network→cache→assets fallback and cache writing internally */
    suspend fun fetchLld(): LldResult {
        val allowNetwork = settingsStore.allowNetwork.first()
        if (allowNetwork) {
            runCatching { lldDataSource.fetchOnline() }
                .onSuccess { (json, lld) ->
                    runCatching { LldManager.saveLldJsonFileOrThrow(json) }
                    return LldResult.Online(lld)
                }
        }
        return fetchOffline()   // file → assets fallback, including copyJsonIfNeeded
    }
}
```

HomeViewModel only calls `fetchLld()`; SettingsRepository's assets version read also goes through
the same interface exposed by LldManager, so the version-comparison rule exists only once.

---

### S5 🟢 `HomeRepository.mounts` lazy cache diverges from its source

```kotlin
// ui/home/repository/HomeRepository.kt:392
private val mounts by lazy { mountDataSource.getMounts() }
```

**Problem:** the Repository instance caches a mount snapshot; on pull-to-refresh every other piece
of data is re-collected, but mounts uses the stale first copy — refresh semantics are incomplete,
and the cached copy diverges from the real state over time.

**Refactoring plan:** delete the `lazy` and fetch on demand in `detectSar()`/`detectApex()`; mount
data is shell output, so the cost is acceptable. If caching is truly needed, invalidate it
uniformly at the `refresh()` entry point rather than tying it to the instance lifecycle.

---

### S6 🟢 `detectWebView` has two similarly-spelled variables; the legacy path reads the wrong one

```kotlin
// ui/home/repository/HomeRepository.kt
var builtInResult = ""
var builtInVersionName = ""                          // :910 outer, used by the final color check at :989
...
packageInfo.versionName?.also {                      // :922-925 only the Android 7+ branch updates the outer variable
    if (Version(it).isHigherThan(builtInVersionName)) { builtInVersionName = it }
}
...
} else {                                             // Android 5 branch
    val buildInVersionName = buildInPackageInfo?.versionName   // :958 local variable (build ≠ built)
    builtInResult = """...$buildInVersionName..."""
}
...
Version(builtInVersionName).isAtLeast(lldWebViewStable)        // :989 always Version("") on Android 5
```

**Consequence:** on Android 5 the final color comparison uses an empty string, so the "is the
built-in WebView version up to date" check is broken.

**Refactoring plan:** unify into a single `var maxBuiltInVersionName: String? = null` that both
branches update; or extract a pure function
`fun maxVersionOf(providers: List<PackageInfo?>): String?` and compute it once at the end of the
function.

---

### S7 🟢 Dual-track state restoration: StateFlow already handles restoration, yet the injected SavedStateHandle is never read or written

The Factories of all four list/settings ViewModels inject a `createSavedStateHandle()` into the
constructor, but the class body has **zero references** to `savedStateHandle` — what actually
handles "restoration after configuration change" is `BaseListViewModel.modelsStateFlow` (a
StateFlow; the ViewModel itself survives) and `SettingsViewModel.version`:

```kotlin
// ui/home/HomeViewModel.kt:29 and 38 — OthersViewModel/PropViewModel/SettingsViewModel are structured identically
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle   // ← injected, but never used anywhere in the class
) : BaseListViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as HomeRepository
                val savedStateHandle = createSavedStateHandle()   // ← obtained for nothing
                HomeViewModel(repository, savedStateHandle)
            }
        }
    }
}
```

**Problem:** nominally there are two state sources (StateFlow instance state + SavedStateHandle
persisted state), but in reality only the StateFlow one works. The SavedStateHandle is dead code,
yet it misleads readers into thinking the list state survives process death — **it does not**.
Contrasted with S2 (`MainViewModel` is exactly the reverse: the SavedStateHandle is in use and the
in-memory `var` is redundant), this shows the project has no unified contract for "who owns
state".

**Refactoring plan:**

- If list state only needs to survive configuration changes (the status quo): delete the 4
  `savedStateHandle` constructor parameters and the `createSavedStateHandle()` calls, making it
  explicit that the StateFlow is the single source of truth;
- If lists should also be restored after process death (more complete, but requires `MyModel` to
  be serializable and loading to be re-triggered on restore): replace `modelsStateFlow`'s
  in-memory field with `savedStateHandle.getStateFlow("models", State.NotInitialized)` so the
  SavedStateHandle truly becomes the SSOT — choose one of the two; they cannot coexist.

---

## 3. UDF Issue Details

### U1 🔴 Static SharedFlow event bus in a companion object + View-layer relay

```kotlin
// ① Static event bus, tied to no ViewModel instance lifecycle — ui/settings/SettingsViewModel.kt:38-43
companion object {
    val scrollBarModeChangedSharedFlow: SharedFlow<String?>
        field = MutableSharedFlow()                  // replay = 0: missed means lost
    val outdatedOrderChangedSharedFlow: SharedFlow<Unit>
        field = MutableSharedFlow()
}

// ② Write side — ui/settings/SettingsFragment.kt:92-97
scrollBarModePref?.setOnPreferenceChangeListener { preference, newValue ->
    listView.setScrollBarMode(newValue as? String)
    settingsViewModel.emitScrollBarModeChangedSharedFlow(newValue as? String)
    true
}

// ③ View-layer relay — ui/home/HomeFragment.kt:40-45 (HomeFragment both receives events and issues commands)
viewLifecycleOwner.lifecycleScope.launch {
    SettingsViewModel.outdatedOrderChangedSharedFlow
        .flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
            listViewModel.payloadOutdatedTargetSdkVersionApk()   // converted into a command call
        }
}
```

**Triple breakage:**

1. "Events" replace "state" for propagation — a replay=0 fire-and-forget: if the list has not
   reached `State.Done`, the patch is simply dropped (HomeViewModel.kt:182-184); the user toggles
   the ordering switch and the UI does not update until the next refresh;
2. Communication between two ViewModels is relayed through the View layer, turning the View into
   a router;
3. The state change (writing SharedPreferences) and the event emission are two independent
   actions — not atomic.

**Refactoring plan:** remove entirely once S1 lands:

```kotlin
// Inside HomeViewModel — setting changes re-derive automatically; no events needed
init {
    viewModelScope.launch {
        combine(
            settingsStore.outdatedOrderByPackage,   // SettingsStore from S1
            refreshTrigger,                          // emitted when refresh() is called
        ) { orderByPackage, _ -> detect(orderByPackage) }
            .collect { modelsStateFlow.value = State.Done(it) }
    }
}

// BaseListFragment — scroll bar mode becomes a Flow exposed by the ViewModel; initial value and updates share one source
init {
    viewLifecycleOwner.lifecycleScope.launch {
        settingsViewModel.scrollBarMode
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .collect { binding.recyclerView.setScrollBarMode(it) }
    }
}
```

---

### U2 🔴 List state is patched by index instead of derived

```kotlin
// ① Mutate a single item by index — ui/base/list/BaseListViewModel.kt:45-60
@MainThread
fun updateModelDetail(targetIndex: Int, newDetail: String) {
    modelsStateFlow.update { state ->
        if (state !is State.Done) return@update state
        val list = state.value
        if (targetIndex !in list.indices) return@update state
        val newList = list.toMutableList()
        newList[targetIndex] = newList[targetIndex].copy(detail = newDetail)
        State.Done(newList)
    }
}

// ② Re-read the file, recompute one item, find its index, patch — ui/home/HomeViewModel.kt:174-196
@MainThread
suspend fun payloadOutdatedTargetSdkVersionApk() {
    val lld = fetchOfflineLldOrNull().lld ?: return
    val newDetail = homeRepository.getOutdatedTargetSdkVersionApkModel(lld).detail
    val state = modelsStateFlow.value
    if (state !is State.Done) return            // ← list not loaded yet: the change is dropped
    ...
    updateModelDetail(targetIndex, newDetail)
}
```

**Problem:** the Home page's final UI state = initial detect result + after-the-fact patches; it
is not a pure-function derivation from (settings + lld + device info), and it depends on events
arriving in the correct order.

**Refactoring plan:** see U1's refactoring code — `combine(settings, data source, refresh signal)`
re-derives the whole thing, recomputing automatically whenever settings change, so there is no
"missed patch" window. Delete `updateModelDetail` and `payloadOutdatedTargetSdkVersionApk`
entirely.

---

### U3 🟡 View layer reads the data layer directly

```kotlin
// ① BaseListFragment.kt:42-44 — the Fragment bypasses the ViewModel and reads prefs directly (initial value)
val scrollBarModeKey = MyApplication.getMyString(R.string.interface_scroll_bar_key)
val scrollBarMode = MyApplication.sharedPreferences.getString(scrollBarModeKey, null)
binding.recyclerView.setScrollBarMode(scrollBarMode)

// ② SettingsFragment.kt:82-85 — the listener calls a global static to change the theme (MyApplication.initTheme reads it yet again at startup)
themeModePref?.setOnPreferenceChangeListener { preference, newValue ->
    MyApplication.setMyTheme(newValue as? String)
    true
}
```

**Refactoring plan:**

- ① The initial value comes from the Flow's first emission instead (collect a Flow that carries a
  default, and delete the separate direct read; see U1's refactoring code);
- ② Theme reading goes uniformly through `SettingsStore.theme`: the Application collects it at
  startup and calls `AppCompatDelegate.setDefaultNightMode`; preference changes only write to
  DataStore, and theme application is done by the collector — exactly one read path and one write
  path.

---

### U4 🔴 Loading-coroutine lifecycle mismatch: unguarded concurrency + stuck-in-Loading bug

```kotlin
// ① init is tied to the view lifecycle — ui/base/list/BaseListFragment.kt:69-71
viewLifecycleOwner.lifecycleScope.launch {
    listViewModel.init(savedInstanceState)
}

// ② Set Loading first, then do the work; the two entry points init/refresh are not mutually exclusive — ui/base/list/BaseListViewModel.kt:19-41
suspend fun init(savedInstanceState: Bundle?) {
    if (hasNoData(savedInstanceState)) {          // mixes the View-layer Bundle with the VM's own state
        setLoading()                               // ← if the coroutine is cancelled, state stays here forever
        val list = collectModels()
        setModels(list)
    }
}
fun refresh() {
    viewModelScope.launch {                        // ← can run concurrently with init, repeating shell/network work
        setLoading()
        val list = collectModels()
        setModels(list)
    }
}
```

**Reproducible bug:** enable network mode, rotate the screen while lld.json is loading →
viewLifecycleScope is cancelled, state stops at `Loading` → when the new view calls
`init(savedInstanceState)` again, both conditions of
`hasNoData = (savedInstanceState == null || value == NotInitialized)` are false → no further
loading → **empty list + a refresh spinner that spins forever** (BaseListFragment.kt:59); only a
manual pull-to-refresh recovers.

**Refactoring plan:**

```kotlin
abstract class BaseListViewModel : BaseViewModel() {
    private var loadJob: Job? = null

    fun load(initial: Boolean = false) {
        if (initial && modelsStateFlow.value != State.NotInitialized) return
        loadJob?.cancel()                          // mutual exclusion: a new load cancels the old one
        loadJob = viewModelScope.launch {          // state transitions belong to the VM's own scope
            modelsStateFlow.value = State.Loading
            runCatching { collectModels() }
                .onSuccess { modelsStateFlow.value = State.Done(it) }
                .onFailure { modelsStateFlow.value = State.Error(it) }   // pairs with O1's Error variant
        }
    }
}

// BaseListFragment — no Bundle passed anymore; the semantics are carried by the VM's own state
viewLifecycleOwner.lifecycleScope.launch {
    listViewModel.load(initial = savedInstanceState == null)
}
```

---

### U5 🟢 Query functions with side effects / one-shot events bypassing state

```kotlin
// ① A read function mutates state — ui/settings/SettingsViewModel.kt:71-86
private var timesLeft = 7
fun getVersionClickedMessage(): Int? {
    if (timesLeft <= 0) return null
    timesLeft--                        // decrements the counter on every "read"
    if (timesLeft > 0) return null
    return R.string.about_version_click
}

// ② The Fragment takes the return value synchronously and toasts it directly — ui/settings/SettingsFragment.kt:159-165
versionPref?.setOnPreferenceClickListener {
    settingsViewModel.getVersionClickedMessage()?.let { this.context?.toast(it) }
    true
}
```

(Same family as ②: `HomeRepository.detectAndroid()` writes a global inside a read function, see
S3.)

**Refactoring plan:**

```kotlin
// One-shot events go out through a Channel; the click enters the VM as an event
private val versionClickEvents = Channel<Int>(Channel.BUFFERED)
val versionClickEvent = versionClickEvents.receiveAsFlow()

private var timesLeft = 7
fun onVersionClicked() {
    if (timesLeft > 0 && --timesLeft == 0) {
        viewModelScope.launch { versionClickEvents.send(R.string.about_version_click) }
    }
}

// SettingsFragment
viewLifecycleOwner.lifecycleScope.launch {
    settingsViewModel.versionClickEvent
        .flowWithLifecycle(viewLifecycleOwner.lifecycle)
        .collect { context?.toast(it) }
}
```

---

### U6 🟢 Fragments reach across layers to read the Activity's private binding

```kotlin
// ui/base/list/BaseListFragment.kt:77 (same at SettingsFragment.kt:67)
(activity as? MainActivity)?.binding?.bottomNavigationView?.doOnLayout { bnv ->
    rv.updatePadding(left = insets.left, right = insets.right, bottom = bnv.height)
}
```

**Problem:** the list's bottom padding depends on the live measurement of the Activity's private
binding, grabbed on the spot via an upcast; the view state has no shared source.

**Refactoring plan:** the bottom bar height is fundamentally an insets problem — handle it
uniformly on MainActivity's fragment container: after `bottomNavigationView` is measured, merge
`(system insets, bar height)` into the container's padding so Fragment layouts naturally avoid
it; delete all `(activity as? MainActivity)` code from the Fragments. Transitional option: the
Activity exposes `bottomBarHeight: StateFlow<Int>` for Fragments to observe.

---

### U7 🟢 `SettingsViewModel.version` loading has no idempotency guard

```kotlin
// ui/settings/SettingsFragment.kt:154-155 — triggered on every onViewCreated
val context = MyApplication.instance
settingsViewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
```

**Problem:** every rotation redoes a round of PackageManager / signature / SHA-256 computation and
overwrites `State.Done` (same "unguarded loading" family as U4, but it only wastes work — it does
not get stuck).

**Refactoring plan:**

```kotlin
// SettingsFragment
if (settingsViewModel.version.value == State.NotInitialized) {
    settingsViewModel.setBuiltInDataVersion()
}
// Also, PackageManager/packageName move inside SettingsRepository instead of being passed in from the View layer
```

---

### U8 🟢 Split identity contract: the Adapter matches by `key`, patches match by index

```kotlin
// ① The Adapter identifies items by key — ui/base/list/MyAdapter.kt:35-39
override fun areItemsTheSame(a: MyModel, b: MyModel) = a.key == b.key

// ② The key is derived from display fields — ui/base/list/MyModel.kt:24-28
val key: String
    get() = when (title) {
        is MyModelTitle.Res -> title.id.toString()
        is MyModelTitle.Raw -> title.text
    }

// ③ The patch uses position instead — ui/base/list/BaseListViewModel.kt:51-57
val targetIndex = list.indexOfFirst { ... }        // HomeViewModel.kt:188
newList[targetIndex] = newList[targetIndex].copy(detail = newDetail)
```

**Problem:** the identity contract is split in two; `payloadOutdatedTargetSdkVersionApk` snapshots
the state to compute the index while `updateModelDetail` re-reads the state internally — if a
concurrent refresh replaces the list between the two reads, the index may point at the wrong row.
Nothing has gone wrong so far only because `detect()`'s append order happens to be fixed.

**Refactoring plan:** dissolved together with U2 (the patch mechanism is deleted entirely). If
localized updates must be kept, switch to a stable identifier:

```kotlin
fun updateModel(type: MyModelType, newDetail: String) {
    modelsStateFlow.update { state ->
        if (state !is State.Done) return@update state
        State.Done(state.value.map {
            if (it.type == type) it.copy(detail = newDetail) else it
        })
    }
}
// MyModel gains an independent id field; the key is no longer derived from title
```

---

## 4. Other Issues (State Purity / Infrastructure)

### O1 🟡 Display snapshots frozen into state; title/detail of the same row resolved at different times

```kotlin
// ① Localized display text is formatted into state at detect time — ui/base/list/MyModelExt.kt:11-18
fun toColoredMyModel(@StringRes titleRes: Int, detail: String?, @AttrRes color: Int): MyModel {
    return MyModel(
        title = MyModelTitle.Res(titleRes),
        detail = detail.toString(),        // ← an already-formatted localized string: a frozen snapshot
        color = color
    )
}

// ② The title, in contrast, is resolved only at bind time — ui/base/list/MyAdapter.kt:26-30
tvTitle.text = when (val title = model.title) {
    is MyModelTitle.Res -> MyApplication.getMyString(title.id)   // ← resolved fresh on every bind
    is MyModelTitle.Raw -> title.text
}
tvDetail.text = model.detail                                    // ← frozen snapshot

// ③ State has no Error variant; errors get stringified and glued into detail — ui/common/StateExt.kt
sealed interface State<out T> {
    data class Done<out T>(val value: T) : State<T>
    data object Loading : State<Nothing>
    data object NotInitialized : State<Nothing>
}
```

**User-visible consequence:** the ViewModel survives configuration changes, so after a system
language switch the same row shows **the title in the new language and the detail in the old one**
until a manual refresh; error states cannot be re-evaluated by the state machine.

**Refactoring plan:**

- `MyModel` stores structured data (`@StringRes + args`, or raw data + a render type), and
  `onBindViewHolder` resolves text uniformly;
- `State` gains `data class Error<out T>(val throwable: Throwable) : State<T>`, separating errors
  from display text (pairs with the U4 refactor);
- This change has a wide blast radius and can be done incrementally: add the Error variant first,
  unify text-resolution timing last.

---

### O2 🟢 `lateinit var instance` global mutable service locators

```kotlin
// base/shell/ShellManager.kt
class ShellManager(shell: IShell) : IShell by shell {
    companion object { lateinit var instance: ShellManager }     // a reassignable global
}
// base/property/PropertyManager.kt is structured the same

// base/MyApplication.kt:74 and 101-103
instance = this@MyApplication
ShellManager.instance = ShellManager(ShellLibSu)
PropertyManager.instance = PropertyManager(PropertyDefault)
```

**Problem:** anyone can reassign them; they create initialization-order dependencies (accessing
them before `Application.onCreate` crashes); and they are the "enabler" of S1/U3's scattered
global access.

**Refactoring plan:** `companion object { val instance: ShellManager by lazy { ... } }` (written
once, immutable), or pass dependencies via constructor injection; converge `MyApplication.instance`
into a `ContextProvider` interface provided by the base module. This is infrastructure work with
the lowest priority, but it is recommended to handle it alongside S1, because together they
determine whether the "read globals anywhere" coding style can be eradicated.

---

## 5. Refactoring Roadmap

### Phase 1: Dissolve the root causes (highest payoff, recommended first)

| Step | Work | Issues resolved |
|---|---|---|
| 1.1 | Introduce `SettingsStore` (DataStore + Flow), delete the 4 direct prefs reads | S1, U3 |
| 1.2 | Delete the companion static SharedFlows, switch Home to `combine`-based derivation, delete the patch machinery | U1, U2, U8 |
| 1.3 | Switch `MainViewModel` to `savedStateHandle.getStateFlow` | S2 |
| 1.4 | Make `myAndroid` immutable; `detectAndroid` no longer writes back to the global | S3 |
| 1.5 | Remove the unused `savedStateHandle` from the 4 ViewModels (or switch to `getStateFlow` to make it a true SSOT) | S7 |

### Phase 2: Fix the actual bug and low-risk cleanup

| Step | Work | Issues resolved |
|---|---|---|
| 2.1 | Unified load entry point in `BaseListViewModel` + `Job` guard + `State.Error` | U4 (stuck-in-Loading bug), O1 (partially) |
| 2.2 | Idempotency guard for `version` | U7 |
| 2.3 | Merge the `detectWebView` variables | S6 |
| 2.4 | Remove the `mounts` cache | S5 |
| 2.5 | Version click becomes a Channel event | U5 |

### Phase 3: Structural cleanup (can be scheduled later)

| Step | Work | Issues resolved |
|---|---|---|
| 3.1 | Consolidate lld into a single `LldRepository` entry point | S4 |
| 3.2 | Unified insets handling; delete `(activity as? MainActivity)` | U6 |
| 3.3 | Unify text-resolution timing (at bind time); make `MyModel` structured | O1 (the rest) |
| 3.4 | Make the service locators immutable | O2 |

**Dependencies:** 1.1 precedes 1.2; 2.1's `State.Error` pairs with O1; the remaining steps are
independent and can run in parallel.

**Verification advice:** regress each step against four scenario families — screen rotation /
system language switch / toggling settings / weak network (with allowNetwork on); the U4 fix can
be verified by directly reproducing "rotate the screen mid-load under weak network".

---

## 6. Complete File Inventory and Conclusion Quick Reference

> All 60 Kotlin files, categorized one by one by module. `SSOT`/`UDF`/`Other` means **the file
> itself contains the corresponding issues**; `Affected` means the file itself is clean but
> reads/writes global state and is impacted by global singletons such as S3; "—" means no SSOT/UDF
> issues.

### app module (`app/src/main/java/net/imknown/android/forefrontinfo/`)

| File | Category | Related issues |
|---|---|---|
| base/MyApplication.kt | SSOT + UDF + Other | S1 U3 O2 |
| ui/MainActivity.kt | UDF | U6 |
| ui/MainViewModel.kt | SSOT | S2 |
| ui/base/BaseFragment.kt | — | |
| ui/base/BaseViewModel.kt | — | |
| ui/base/ext/ToastExt.kt | — | |
| ui/base/ext/ViewBindingExt.kt | — | |
| ui/base/list/BaseListFragment.kt | SSOT + UDF | S1 U1 U3 U4 U6 |
| ui/base/list/BaseListViewModel.kt | UDF | U2 U4 U8 |
| ui/base/list/MyAdapter.kt | UDF + Other | U8 O1 |
| ui/base/list/MyItemDecoration.kt | — | |
| ui/base/list/MyModel.kt | UDF | U8 |
| ui/base/list/MyModelExt.kt | Other | O1 |
| ui/base/list/MyViewHolder.kt | — | |
| ui/common/AndroidVersionExt.kt | SSOT | S3 |
| ui/common/JsonExt.kt | — | |
| ui/common/LldManager.kt | SSOT | S4 |
| ui/common/PropertyExt.kt | — | (depends on O2) |
| ui/common/ShellExt.kt | — | (depends on O2) |
| ui/common/ShellLibSu.kt | — | |
| ui/common/StateExt.kt | Other | O1 |
| ui/common/ViewExt.kt | UDF (consumer side) | U1 |
| ui/home/HomeFragment.kt | UDF | U1 |
| ui/home/HomeViewModel.kt | SSOT + UDF | S1 S4 S7 U1 U2 U8 |
| ui/home/model/BaseInfo.kt | — | |
| ui/home/model/Lld.kt | — | |
| ui/home/repository/HomeRepository.kt | SSOT + Other | S1 S3 S5 S6 O1 |
| ui/home/datasource/AndroidDataSource.kt | — | |
| ui/home/datasource/LldDataSource.kt | — | S4 (involved) |
| ui/home/datasource/MountDataSource.kt | — | S5 (involved) |
| ui/others/OthersFragment.kt | — | |
| ui/others/OthersViewModel.kt | SSOT + Affected | S7 (affected by S3) |
| ui/others/repository/OthersRepository.kt | Affected | S3 (affected) |
| ui/others/datasource/ArchitectureDataSource.kt | — | |
| ui/others/datasource/BasicDataSource.kt | — | |
| ui/others/datasource/FingerprintDataSource.kt | — | |
| ui/others/datasource/KernelDataSource.kt | — | |
| ui/others/datasource/OthersDataSource.kt | — | |
| ui/others/datasource/RomDataSource.kt | — | |
| ui/prop/PropFragment.kt | — | |
| ui/prop/PropViewModel.kt | SSOT | S7 |
| ui/prop/repository/PropRepository.kt | — | |
| ui/prop/datasource/PropertiesDataSource.kt | — | |
| ui/prop/datasource/SettingsDataSource.kt | — | |
| ui/settings/SettingsFragment.kt | UDF | U1 U3 U5 U6 U7 |
| ui/settings/SettingsViewModel.kt | SSOT + UDF | S7 U1 U5 U7 |
| ui/settings/repository/SettingsRepository.kt | SSOT | S4 |
| ui/settings/datasource/AppInfoDataSource.kt | — | |
| ui/settings/datasource/FingerprintDataSource.kt | — | |

### base module (`base/src/main/java/net/imknown/android/forefrontinfo/base/`)

| File | Category | Related issues |
|---|---|---|
| extension/CollectionExt.kt | — | |
| extension/DateTimeExt.kt | — | |
| extension/ExceptionExt.kt | — | |
| property/IProperty.kt | — | |
| property/PropertyManager.kt | Other | O2 |
| property/impl/PropertyDefault.kt | — | |
| shell/IShell.kt | — | |
| shell/ShellManager.kt | Other | O2 |
| shell/ShellResult.kt | — | |
| shell/impl/ShellDefault.kt | — | |

> Additionally, `binderDetector/` (an NDK library with no Kotlin state) and `build-logic/` (Gradle
> convention plugins, unrelated to runtime state) are not covered.

### Statistics

- **SSOT**: S1–S7, 7 issues (S1 and S3 are root causes);
- **UDF**: U1–U8, 8 issues (U1, U2, U4 are high severity);
- **Other**: O1 and O2, 2 issues;
- Files with the most issues: `HomeRepository.kt` (S1 S3 S5 S6 O1), `BaseListFragment.kt`
  (S1 U1 U3 U4 U6), `HomeViewModel.kt` (S1 S4 S7 U1 U2 U8), `SettingsViewModel.kt` (S7 U1 U5 U7).

---

## Revision History

| Date | Changes |
|---|---|
| 2026-09-03 | Initial version: 16 issues (SSOT 6 / UDF 8 / Other 2). |
| 2026-09-03 | Re-reviewed all 60 Kotlin files on the develop branch: ① added S7 (dual-track state restoration; the SavedStateHandle injected into 4 ViewModels is unused); ② added the category criteria (the basis for splitting SSOT/UDF/Other); ③ appended Section 6, "Complete File Inventory and Conclusion Quick Reference" (per-file categorization + statistics). |
| 2026-09-03 | Translated to English (this file replaces the Chinese original). |
