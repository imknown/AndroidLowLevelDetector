# Pull-to-Refresh Stuck Issue Analysis Report

**Project**: AndroidLowLevelDetector  
**Issue**: On first launch, HomeFragment auto-loads its list (pull-to-refresh spinner showing). If the user toggles the system dark/light mode while the load is still in flight, MainActivity is destroyed and recreated, after which HomeFragment stays stuck on the refresh spinner forever and the list never loads. OthersFragment / PropFragment have the same problem (they share the same base class logic).  
**Report date**: 2026-09-02

---

## 1. Symptoms

1. Cold-start the app; HomeFragment auto-loads its list on first entry, and the SwipeRefreshLayout shows the spinner.
2. While the load has **not yet finished** ("toggling quickly" is the key), switch the system dark/light mode.
3. The `uiMode` configuration change → `MainActivity` is destroyed and recreated.
4. After the recreation, HomeFragment's pull-to-refresh spinner **never stops**, and the data is never refreshed.

Key point: if the mode is switched after the load has already completed, everything works fine; only a load interrupted mid-flight by the recreation gets stuck. This timing characteristic is an important clue for locating the root cause.

---

## 2. Investigation Trail (Code-Walkthrough Path)

Following the data flow from the View layer down to the ViewModel, then comparing all list Fragments horizontally:

### Clue 1: View layer — `BaseListFragment`

`app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/BaseListFragment.kt`

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    listViewModel.modelsStateFlow.flowWithLifecycle(
        viewLifecycleOwner.lifecycle
    ).collect { stateMyModels ->
        when (stateMyModels) {
            State.NotInitialized -> return@collect
            State.Loading -> binding.swipeRefreshLayout.isRefreshing = true
            is State.Done -> {
                myAdapter.submitList(stateMyModels.value)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
}

viewLifecycleOwner.lifecycleScope.launch {
    listViewModel.init(savedInstanceState)   // ← note: runs in the viewLifecycleOwner scope
}
```

Observations:

- State collection uses `flowWithLifecycle(viewLifecycleOwner.lifecycle)` — it pauses collection when the UI stops and cancels on destroy. Correct in itself.
- **The initial load `init(savedInstanceState)` is wrapped in a `viewLifecycleOwner.lifecycleScope.launch`**. This is a suspicious point: coroutines in the View-lifecycle scope get cancelled when the Fragment's view is destroyed.
- The SwipeRefreshLayout's `setOnRefreshListener { listViewModel.refresh() }` goes through a separate load path (`refresh()` uses `viewModelScope` internally).

### Clue 2: ViewModel layer — `BaseListViewModel`

`app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/BaseListViewModel.kt`

```kotlin
val modelsStateFlow: StateFlow<State<List<MyModel>>>
    field = MutableStateFlow<State<List<MyModel>>>(State.NotInitialized)

abstract suspend fun collectModels(): List<MyModel>

suspend fun init(savedInstanceState: Bundle?) {
    // When activity is recreated, use StateFlow to restore the data
    if (hasNoData(savedInstanceState)) {
        setLoading()
        val list = collectModels()
        setModels(list)
    }
}

private fun hasNoData(savedInstanceState: Bundle?) =
    savedInstanceState == null || modelsStateFlow.value == State.NotInitialized

fun refresh() {
    viewModelScope.launch {          // ← contrast: refresh uses viewModelScope
        setLoading()
        val list = collectModels()
        setModels(list)
    }
}
```

Observations:

- `modelsStateFlow` is held in the ViewModel and survives Activity recreation (the Fragment's ViewModelStore is retained across configuration changes).
- **`init` is a suspend function that does not start its own coroutine** — it lives in whichever coroutine scope calls it. The Fragment happens to call it from `viewLifecycleOwner.lifecycleScope`.
- The `hasNoData` logic: `savedInstanceState == null` (first launch) **or** the current state is `NotInitialized` means "no data, need to load". **No branch handles "the current state is Loading"** — a major clue.
- `refresh()` contrasts sharply with `init`: `refresh()` uses `viewModelScope` internally and does not depend on the View being alive.

### Clue 3: Data-source latency — why Home reproduces most easily

`HomeViewModel.collectModels()` → `tryDetectOnline()`:

- `homeRepository.fetchOnlineLldJsonStringOrThrow()` performs a **network request** on `Dispatchers.IO` (when settings allow networking);
- after that come JSON parsing, file saving (`LldManager.saveLldJsonFileOrThrow`), asset file reading, and a whole chain of `withContext(Dispatchers.IO)` suspension points.

The network request takes a long time → a large window → "quickly toggling dark/light mode" easily interrupts it. OthersFragment / PropFragment's `collectModels()` also reads Settings.Global/Secure/System and runs `getprop` shell commands (`ShellLibSu` → `Shell.cmd(cmd).exec()`), so they have the same window — it is just usually shorter and harder to hit.

### Clue 4: Configuration changes always recreate the Activity

`app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".ui.MainActivity"
    android:exported="true"
    android:launchMode="singleTop" />
```

`android:configChanges="uiMode"` is not declared, so a `uiMode` change (dark/light switch) goes through the full destroy-and-recreate flow. This is the trigger condition, but **not the root cause** — any normal app should be able to recover after a configuration change.

### Clue 5: Fragment restoration and state recovery

`MainActivity.onCreate`:

```kotlin
if (savedInstanceState == null) {
    supportFragmentManager.switch(R.id.navigation_home, true)
}
```

On configuration-change recreation, `savedInstanceState != null`, and the FragmentManager restores the Fragments automatically (Home/Others/Prop/Settings are added with tag = navigation id, toggled via `show/hide`). The new Fragment view goes through `onViewCreated` again → re-collects `modelsStateFlow`, and **calls `listViewModel.init(savedInstanceState)` again** (with `savedInstanceState` now non-null).

### Clue 6: Manual pull-to-refresh is unaffected (counter-evidence)

The user-reported symptom is "initial auto-load interrupted → stuck", while manual pull-to-refresh (`refresh()`) does not get stuck even if the mode is switched mid-flight — after recreation the new View collects either the in-flight `Loading` (the coroutine is still alive and pushes `Done` when data arrives) or `Done`. **This proves by contradiction: the problem is not in state collection, not in SwipeRefreshLayout, not in the data source — it is in the coroutine scope of the initial load.**

---

## 3. Complete Failure Timeline (Reasoning)

**T0 — Cold start (`savedInstanceState == null`)**

1. `HomeFragment.onViewCreated` → `viewLifecycleOwner.lifecycleScope.launch { listViewModel.init(null) }`.
2. `hasNoData(null)` → `true`. Sets `modelsStateFlow.value = State.Loading` (spinner starts).
3. Enters `collectModels()` → `tryDetectOnline()` → suspends at `withContext(Dispatchers.IO) { fetchOnlineLldJsonStringOrThrow() }` waiting for the network.

**T1 — User toggles dark/light mode (load still in flight)**

4. `uiMode` configuration change → old `MainActivity.onDestroy` → Fragment view destroyed → `viewLifecycleOwner` lifecycle enters `DESTROYED`.
5. **All coroutines in `viewLifecycleOwner.lifecycleScope` are cancelled** — the `init` coroutine throws `CancellationException` at the `withContext(Dispatchers.IO)` suspension point.
6. `collectModels()` is abandoned mid-flight; `setModels(list)` **never executes**.
7. But `HomeViewModel` (along with `modelsStateFlow`) survives in the ViewModelStore — **`modelsStateFlow.value` is permanently stuck at `State.Loading`**.

**T2 — Activity recreation completes**

8. The FragmentManager restores HomeFragment; the new view's `onViewCreated`:
   - the collector receives the sticky `State.Loading` → `isRefreshing = true` (spinner keeps spinning, looking like a "normal in-flight load");
   - calls `init(savedInstanceState)` again, this time with `savedInstanceState != null`.
9. `hasNoData` evaluation: `savedInstanceState == null` → `false`; `modelsStateFlow.value == State.NotInitialized` → `false` (it is `Loading`).
10. **`hasNoData` returns `false` → load skipped**. No code path will ever push the state to `Done` again.

**T3 — Deadlock state**

11. The SwipeRefreshLayout spins forever. The user can manually pull to refresh to trigger `refresh()` and escape, but the auto-load is permanently lost.

### Why only "toggling quickly" reproduces it

If T1 happens after `collectModels()` has completed (state already `Done`), then after recreation `init` is likewise short-circuited by `hasNoData`, but the collector receives `Done` → the list renders normally, no anomaly. Only a load interrupted **mid-flight** (state stuck at `Loading`) enters the stuck state. This explains the timing sensitivity of the issue, and matches the user's description that "it existed very early — the April code could already reproduce it" — this base-class logic has carried the defect since it was introduced.

---

## 4. Investigation Findings

### Direct cause

After the Activity is recreated, when `BaseListFragment.onViewCreated` calls `listViewModel.init(savedInstanceState)` again, `BaseListViewModel.hasNoData()` returns `false` (`savedInstanceState != null` and the state is not `NotInitialized` but the residual `Loading`), so the data load is permanently skipped, `modelsStateFlow` stays at `State.Loading`, and the pull-to-refresh spinner spins forever.

### Root cause (two defects stacked)

1. **Wrong coroutine scope**: the initial data load `init()` is a suspend function, yet it is executed in `viewLifecycleOwner.lifecycleScope` (`BaseListFragment.kt:69-71`). Coroutines in the View-lifecycle scope are cancelled when the view is destroyed, so a "one-time initialization load" that should survive across configuration changes gets interrupted. Contrast with `refresh()`, which uses `viewModelScope` and has no such problem — two coexisting scopes in the same class is itself an inconsistent design.

2. **Incomplete state check**: `hasNoData()` only treats `State.NotInitialized` as "no data"; it does not consider the case of "the previous load was cancelled, the state is residual `Loading`, and no coroutine is in flight". `Loading` is a **transient** state, yet it is treated as the equivalent of "data ready" for the short-circuit check.

One-sentence summary: **the view-scoped initial-load coroutine was cancelled by the recreation, leaving behind a `Loading` state that is neither data nor claimed by anyone; the recovery check misreads this orphaned state as "already has data", so a load is never triggered again.**

### Side findings (mentioned in passing, not the root cause of this issue)

- `catch (e: Exception)` in `HomeViewModel.tryDetectOnline()` / `fetchOfflineLldOrNull()` and similar places swallows `CancellationException`. In this case a subsequent suspension point throws the cancellation again, so it still ends in cancellation and does not affect the conclusion above — but it is a coroutine anti-pattern (structured cancellation should rethrow `CancellationException`).
- `init`/`refresh` have no concurrent deduplication — if `onViewCreated` is re-entered quickly (e.g. another configuration change), `collectModels()` could be triggered twice concurrently. The fix below resolves this as a side effect.

---

## 5. Fix Proposal

### Proposal: move the initial load into `viewModelScope` + Job deduplication (recommended)

Core idea: let the initial load and the manual refresh share the same `viewModelScope` load path, making it immune to Activity recreation; use a Job reference to decide "is a load already in flight" instead of guessing from the state.

#### Change 1: `BaseListViewModel.kt`

```kotlin
import kotlinx.coroutines.Job

abstract class BaseListViewModel : BaseViewModel() {
    val modelsStateFlow: StateFlow<State<List<MyModel>>>
        field = MutableStateFlow<State<List<MyModel>>>(State.NotInitialized)

    abstract suspend fun collectModels(): List<MyModel>

    private var loadJob: Job? = null

    fun init(savedInstanceState: Bundle?) {          // no longer suspend
        // First entry (no savedInstanceState) or state never initialized → need to load;
        // if loadJob is still active, the previous load is still in flight
        // (e.g. the pre-recreation load) — do not trigger again
        if (hasNoData(savedInstanceState) && loadJob?.isActive != true) {
            loadJob = viewModelScope.launch {
                setLoading()
                setModels(collectModels())
            }
        }
    }

    private fun hasNoData(savedInstanceState: Bundle?) =
        savedInstanceState == null || modelsStateFlow.value == State.NotInitialized

    fun refresh() {
        if (loadJob?.isActive != true) {             // also eliminates concurrent duplicate loads
            loadJob = viewModelScope.launch {
                setLoading()
                setModels(collectModels())
            }
        }
    }
    // ...everything else unchanged
}
```

#### Change 2: `BaseListFragment.kt`

```kotlin
// Before:
// viewLifecycleOwner.lifecycleScope.launch {
//     listViewModel.init(savedInstanceState)
// }

// After (init is no longer suspend; a direct synchronous call is enough):
listViewModel.init(savedInstanceState)
```

### Post-fix behavior verification (reasoned)

| Scenario | Before fix | After fix |
|---|---|---|
| Toggle dark/light mode during first load | Stuck on loading | The load coroutine lives in `viewModelScope` and pushes `Done` when it completes; the new View after recreation collects `Done`, the spinner stops, the list renders ✓ |
| Toggle mode after load completes | Normal (recovered from `Done`) | Unchanged, normal ✓ |
| Toggle mode during manual pull-to-refresh | Normal | Unchanged, normal ✓ |
| Multiple rapid recreations (extreme) | May load concurrently multiple times | Deduplicated by `loadJob?.isActive`; at most one in-flight load ✓ |
| Recovery after process death | `NotInitialized` → loads normally again | Unchanged ✓ |

### Alternatives (not recommended, listed only)

- **Treat `Loading` as "no data" too in `hasNoData`**: treats the symptom, not the disease. The initial load would still be cancelled by the view scope and re-triggered after recreation — functional, but the initial load result is needlessly discarded and redone (the network request is re-sent), and it does not fix the `init` coroutine cancellation itself.
- **Add `android:configChanges="uiMode"` to the Manifest**: avoidance, not a fix. On dark/light switches the app must handle all resource refreshing itself, against Android's recommendation — and other configuration changes (language, screen size, etc.) would still trigger the same stuck state.

---

## 6. Files Involved

| File | Role |
|---|---|
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/BaseListFragment.kt` | Initial-load call site (where the wrong scope lives), state collection and UI feedback |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/BaseListViewModel.kt` | `init`/`hasNoData`/`refresh`, `modelsStateFlow` (where the root cause lives) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/HomeViewModel.kt` | `collectModels` network request + multiple IO segments (determines the size of the reproduction window) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/others/OthersViewModel.kt` | Same kind of `collectModels` (Settings reads + shell) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/prop/PropViewModel.kt` | Same kind of `collectModels` (System.getProperties + getprop) |
| `app/src/main/AndroidManifest.xml` | No `configChanges` declared; confirms the recreation path |

The three business Fragments (Home/Others/Prop) need no changes; the fix is fully contained in the two base classes `BaseListViewModel` + `BaseListFragment`.
