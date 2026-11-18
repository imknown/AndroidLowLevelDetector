package net.imknown.android.forefrontinfo.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModelType
import net.imknown.android.forefrontinfo.ui.home.detect.HomeDetectionRequest
import net.imknown.android.forefrontinfo.ui.home.detect.HomeDetectionResult
import net.imknown.android.forefrontinfo.ui.home.detect.HomeDetectorCatalog
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsStore

// Stable (not Immutable): instance identity never changes and UI-visible state lives in the
// observed StateFlow; SavedStateHandle is restore-only storage, never read for composition,
// so promising stability is safe (same as BaseListViewModel).
// Metro wiring: the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseListViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Stable
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class HomeViewModel(
    private val detectorCatalog: HomeDetectorCatalog,
    private val settingsStore: SettingsStore
) : BaseListViewModel() {

    // The last completed run. Its rows are what is on screen, and the domain object it kept for
    // the outdated-target row is what the order switch re-renders from.
    private var lastRun: HomeDetectionResult? = null

    init {
        // The order switch is a display parameter, not a detection input: switching it re-renders
        // one row against the last run's data and never starts a run, requests the network or
        // touches the disk. With nothing on screen it does nothing, and the next run renders that
        // row with whatever value is current at that moment. drop(1) skips the StateFlow's
        // initial value, which is the value the first run already used.
        viewModelScope.launch {
            settingsStore.outdatedOrderFirst.drop(1).collect { orderByPackageNameFirst ->
                val run = lastRun ?: return@collect
                if (modelsStateFlow.value == null) return@collect
                val detail = detectorCatalog.outdatedRowDetail(run, orderByPackageNameFirst)
                    ?: return@collect
                updateModelDetail(MyModelType.OutdatedTargetSdkApk, detail)
            }
        }
    }

    override suspend fun collectModels(): List<MyModel> {
        // One-shot read of the hot flow's current value: seeded synchronously at store
        // construction, so first() returns immediately without suspending
        val allowNetwork = settingsStore.allowNetworkData.first()

        val run = detectorCatalog.detect(HomeDetectionRequest(allowNetwork))
        lastRun = run

        // The run rendered that row with the value it read when it started. If the switch moved
        // while the run was going, render that one row again with the value that is current now:
        // the row must show the switch as it stands when the data arrives. Pure sorting, and the
        // detection is reused, so nothing is fetched twice.
        val currentOrder = settingsStore.outdatedOrderFirst.value
        return if (run.orderByPackageNameFirst == currentOrder) {
            run.rows
        } else {
            detectorCatalog.rowsWithOutdatedRow(run, currentOrder)
        }
    }
}
