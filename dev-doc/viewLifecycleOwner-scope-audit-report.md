# viewLifecycleOwner Scope Audit Report

**Project**: AndroidLowLevelDetector  
**Issue**: Following the root-cause conclusion of the *Pull-to-Refresh Stuck Analysis Report* (a view-scoped coroutine cancelled by recreation + a residual state nobody claims), audit every remaining use of `viewLifecycleOwner.lifecycleScope` in Fragments for the same family of defects.  
**Summary**: 5 use sites audited (3 files). **1 same-family defect found** — the coroutine in HomeFragment that collects the "outdated APK ordering" setting change. It does not get stuck (the state rests in the terminal `Done`, not the transient `Loading`), but it can **silently drop one list-item re-sort**, until the user manually refreshes or toggles the switch again. All other use sites (besides the `init` call site already covered by the main report) are safe.  
**Report date**: 2026-09-03

---

## 1. Method and Decision Rule

The main report's root cause can be distilled into one decision rule:

> `viewLifecycleOwner.lifecycleScope` is only suitable for "**observing replayable state + idempotently rendering the UI**". When a coroutine carries either of the following kinds of work, it carries the same family of risk:
>
> 1. **A one-time task that must run to completion** (loading, recomputation, state writes) — view destruction cuts it off at a suspension point, leaving it half-done;
> 2. **A non-replayable event source** (a replay=0 SharedFlow) — if the collector is not present, the event is lost forever; when the event carries a state change, losing it means state divergence.

**Search scope**: all `*.kt` in the repo (excluding `build/`), keywords `lifecycleScope` / `launchWhen*` / `repeatOnLifecycle` / `GlobalScope` / `runBlocking`.

**Search result**: no `launchWhen*`, `repeatOnLifecycle`, `GlobalScope`, or `runBlocking`; every `lifecycleScope` is `viewLifecycleOwner.lifecycleScope` — 5 launches in total, across 3 files.

---

## 2. Full Use-Site Inventory and Verdicts

| # | Location | What it does | Verdict |
|---|---|---|---|
| 1 | `BaseListFragment.kt:46-50` | Collects the scroll-bar-mode SharedFlow, updates the RecyclerView | Safe (see 2.1) |
| 2 | `BaseListFragment.kt:53-67` | Collects `modelsStateFlow` (StateFlow), drives the list and spinner | Safe (main report Clue 1 already confirmed "correct in itself") |
| 3 | `BaseListFragment.kt:69-71` | `listViewModel.init(savedInstanceState)` | **The bug from the main report itself** — not re-analyzed here |
| 4 | `HomeFragment.kt:40-45` | Collects the ordering-switch SharedFlow → `payloadOutdatedTargetSdkVersionApk()` recomputes a list item | **Same-family defect (this report's finding)** |
| 5 | `SettingsFragment.kt:129-152` | Collects the `version` StateFlow, updates the About-page version summary | Safe (see 2.2) |

### 2.1 Why the scroll-bar collector (#1) is safe

Its event source is likewise a replay=0 SharedFlow and could in theory drop events, but it satisfies two exemption conditions:

- **onViewCreated re-reads the data source every time**: `BaseListFragment.kt:42-44` reads the current value synchronously from SharedPreferences and applies it **before** starting the collection. Even if an event is lost during a view-recreation gap, the new view already holds the latest preference — no information loss;
- **The collector only does idempotent UI rendering** (`setScrollBarMode`) and writes no state.

Note this is an **implicit dependency**: the safety hinges on the "onViewCreated re-reads the preference" lines existing. If someone later removes lines 42-44 and relies solely on the event flow, this site degrades into the same problem as #4.

### 2.2 Why the version-info collector (#5) is safe

- The source is a **StateFlow (sticky)**; a newly subscribed view always gets the latest value — no lost events;
- The collector only does idempotent rendering (updating `versionPref.summary`);
- The producer `setBuiltInDataVersion` (`SettingsFragment.kt:155`) runs in `viewModelScope` — the correct scope. It re-runs on every onViewCreated, which is harmless redundancy (a local PackageManager query, idempotent and fast).

---

## 3. The Finding: the HomeFragment Ordering-Recompute Collector (#4)

### Current code

`app/src/main/java/net/imknown/android/forefrontinfo/ui/home/HomeFragment.kt:40-45`

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    val flow = SettingsViewModel.outdatedOrderChangedSharedFlow
    flow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
        listViewModel.payloadOutdatedTargetSdkVersionApk()   // ← one-time recompute task
    }
}
```

`app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsViewModel.kt:41-42, 51-55`

```kotlin
val outdatedOrderChangedSharedFlow: SharedFlow<Unit>
    field = MutableSharedFlow()          // ← replay = 0; events are not replayed; dropped with no subscriber

fun emitOutdatedOrderChangedSharedFlow() {
    viewModelScope.launch {
        outdatedOrderChangedSharedFlow.emit(Unit)
    }
}
```

`app/src/main/java/net/imknown/android/forefrontinfo/ui/home/HomeViewModel.kt:174-196` (excerpt)

```kotlin
@MainThread
suspend fun payloadOutdatedTargetSdkVersionApk() {
    val lld = fetchOfflineLldOrNull().lld          // ← suspension point: IO file read + JSON parse
        ?: return

    val newDetail = withContext(Dispatchers.Default) {
        homeRepository.getOutdatedTargetSdkVersionApkModel(lld).detail
        // ↑ suspension point: full scan of installed applications
        //   (getInstalledApplications + sort + string building);
        //   non-trivial duration with many packages → a decent race window
    }

    val state = modelsStateFlow.value
    if (state !is State.Done) {
        return
    }
    // ...find the OutdatedTargetSdkApk entry, then
    updateModelDetail(targetIndex, newDetail)      // ← the sole state write-back point
}
```

### Observations

- **The event is non-replayable**: `MutableSharedFlow()` defaults to replay=0 with no buffer. It is not an event bus — if no subscriber is alive at emit time, the value simply evaporates. HomeFragment's subscription is tied to the view lifecycle, so a no-subscriber window exists between view destruction and the new view's re-subscription.
- **The collector runs a one-time state-mutation task**: `payloadOutdatedTargetSdkVersionApk` is a suspend function with multiple `withContext` suspension points that finally mutates `modelsStateFlow` via `updateModelDetail` — exactly risk category 1 of the decision rule, isomorphic to the main report's `init()`.
- **No fallback re-read**: unlike the scroll-bar collector, onViewCreated here **does not** re-read the ordering preference, nor re-run the recompute after recreation. Event delivery and consumption is the only trigger path for the state update — lose the event, and the state stays at the old ordering.
- **Why it normally works**: MainActivity manages the four Fragments with add + show/hide (`MainActivity.kt:91-112`); a hidden Fragment's **lifecycle stays at RESUMED**, so its collector is never paused. The common path — "toggle the switch on the Settings page → hidden Home receives the event and recomputes" — works perfectly. The problem only surfaces when **view destruction** (configuration-change recreation, Activity destruction) intervenes.

### Two loss paths

**Path A — the event is never delivered (SharedFlow drop)**: the user toggles the switch → `emitOutdatedOrderChangedSharedFlow` queues the emit onto the main thread → if that coroutine actually runs after the old view-scoped collector has been cancelled by view destruction but before the new view's collector registers (the recreation window), the replay=0 SharedFlow drops the value outright. The event evaporates; the recompute never starts.

**Path B — delivered, then cancelled mid-computation (same mechanism as the main report)**: the event is delivered and the recompute starts → a configuration change (e.g. toggling dark/light mode) → `viewLifecycleOwner` enters DESTROYED → the collecting coroutine throws `CancellationException` at a `withContext` suspension point → `updateModelDetail` never executes. And the event has already been consumed — there is no replay.

### Complete failure timeline (Path B, reasoned)

**T0 — the list has finished loading (`Done`); the user is on the Settings page**

1. Toggle "order outdated APKs by package name first" → the preference is persisted → `emitOutdatedOrderChangedSharedFlow()`.
2. HomeFragment is hidden but still RESUMED → the collector receives the event → `payloadOutdatedTargetSdkVersionApk()` starts: read the offline lld JSON (IO) → scan all installed applications (Default).

**T1 — the user toggles dark/light mode (recompute still in flight)**

3. `uiMode` configuration change → old `MainActivity` destroyed → HomeFragment's view destroyed → the view-scoped coroutine is cancelled at a suspension point.
4. `updateModelDetail` never executes; `modelsStateFlow` stays at `Done` (the old-ordered list).

**T2 — Activity recreation completes**

5. The new view's `onViewCreated` → the collector receives the sticky `Done` → renders the **old-ordered** detail normally.
6. `init(savedInstanceState)` is called → `hasNoData` returns `false` (`savedInstanceState != null` and the state is `Done`) → load skipped. No path re-reads the ordering preference.

**T3 — silent divergence state**

7. The list looks completely "normal": no spinner, fully interactive — except the "outdated targetSdk APK" entry's detail is still in the old ordering. Toggling the switch again or pulling to refresh escapes the state.

### Why the severity is far lower than the main bug

1. **Terminal vs transient state**: the residue is `Done` (a stable terminal state), not `Loading` (transient). The UI renders normally and the spinner stops — no glaring "infinite loading" failure; only a close look at the ordering reveals the divergence — which also makes it **more insidious**.
2. **Self-healing**: toggling the switch again or pulling to refresh (both `collectModels`-based, and it reads the latest preference) recovers; the main bug could only be escaped by manual refresh.
3. **Narrow trigger surface**: it requires "event emitted / recompute in flight" to collide precisely with "view destruction", and it affects only one page and one list item.

---

## 4. Side Findings (mentioned in passing, not the root cause here)

- **Loading-guard race**: the trailing `if (state !is State.Done) return` in `payloadOutdatedTargetSdkVersionApk` means — if the switch is toggled while the initial load happens to be in flight (state `Loading`) and the recompute finishes before the load, the re-sort is silently dropped; correctness then depends on whether the in-flight load reads the preference after it was written — pure timing. With the main report's `loadJob` scheme, the payload can first `loadJob?.join()` before checking the state, eliminating the race entirely.
- **A companion-object SharedFlow used as an event bus** is an anti-pattern: process-level lifetime, no replay, dropped without subscribers. In this case it is the enabler of Path A. The fix below closes the gap on the subscriber side and leaves it untouched.
- `catch (e: Exception)` in `HomeViewModel.tryDetectOnline()` / `fetchOfflineLldOrNull()` and similar places swallows `CancellationException` — already recorded in the main report; Path B here is affected the same way (the cancellation thrown at the first suspension point is swallowed by a catch, and only a later suspension point's second throw finally cancels). The conclusion stands.

---

## 5. Fix Proposal

### Proposal: move the collection into HomeViewModel's viewModelScope (recommended)

The core idea matches the main report: let the "one-time task that must complete" live in the ViewModel scope, immune to view destruction. `outdatedOrderChangedSharedFlow` hangs off the `SettingsViewModel` companion object, so `HomeViewModel` can subscribe directly without holding a SettingsViewModel instance.

#### Change 1: `HomeViewModel.kt`

```kotlin
init {
    viewModelScope.launch {
        SettingsViewModel.outdatedOrderChangedSharedFlow.collect {
            payloadOutdatedTargetSdkVersionApk()   // can also be made private along the way
        }
    }
}
```

The subscription is established as soon as HomeViewModel is constructed. HomeViewModel is Fragment-scoped and lives as long as the HomeFragment instance — it survives both the show/hide pattern and configuration changes, so Path A's no-subscriber window and Path B's cancellation both cease to exist.

#### Change 2: `HomeFragment.kt`

Delete the `onViewCreated` override (this collector is its only content); HomeFragment returns to pure base-class behavior.

### Post-fix behavior verification (reasoned)

| Scenario | Before fix | After fix |
|---|---|---|
| Toggle dark/light mode during the recompute, after the switch was toggled (Path B) | Detail stays at the old ordering until manual refresh / re-toggle | The recompute lives in `viewModelScope` and pushes a new `Done` on completion; the new view after recreation collects and renders the new ordering ✓ |
| Event emitted during the recreation window (Path A) | Dropped by the replay=0 SharedFlow; same old-ordering residue | HomeViewModel survives recreation; the subscription never breaks; the event is delivered ✓ |
| Normal switch toggle (no recreation) | Normal | Unchanged ✓ (hidden Fragments stay RESUMED; the collector is present) |
| Recovery after process death | First `init` reads the latest preference — normal | Unchanged ✓ |
| Switch toggled while the initial load is in flight | May be silently dropped by the Loading guard (side finding) | Still present; recommended to combine with the main report's `loadJob` scheme — have the payload `join()` the in-flight load before checking state |

### Alternatives (not recommended, listed only)

- **Give `outdatedOrderChangedSharedFlow` replay=1 (or convert to StateFlow)**: only plugs Path A, not Path B (the view-scoped collector can still be cancelled mid-computation) — treats the symptom, not the disease.
- **Re-run the payload once per onViewCreated on the `Done` state**: would self-heal the divergence, but pays a full installed-apps scan on every recreation — trading sizable repeated cost for correctness; the wrong direction.

---

## 6. Files Involved

| File | Role |
|---|---|
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/HomeFragment.kt` | Where the problem coroutine lives (view-scoped collection + one-time recompute trigger); the deletion point of the fix |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/HomeViewModel.kt` | `payloadOutdatedTargetSdkVersionApk` (the cancelled task itself); where the fix lands |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsViewModel.kt` | The no-replay SharedFlow event source (Path A enabler) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/repository/HomeRepository.kt` | `getOutdatedTargetSdkVersionApkModel` full package scan (determines the size of the race window) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/MainActivity.kt` | Fragments managed via show/hide (hidden ones stay RESUMED — explains why the common path works) |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/BaseListFragment.kt` | The other 2 use sites (audit passed); the `init` call site is the main report's bug itself |
| `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsFragment.kt` | The remaining 1 use site (audit passed) |

One-sentence summary: **of the 5 `viewLifecycleOwner.lifecycleScope` uses in the repo, only HomeFragment puts "a state recompute triggered by a non-replayable event" inside the view scope — the same root as the main bug (view lifetime carrying logic that must survive), but because the residue is the terminal `Done` rather than the transient `Loading`, the consequence downgrades from "stuck forever" to "silently dropping one re-sort". The fix is identical: move it into `viewModelScope`.**
