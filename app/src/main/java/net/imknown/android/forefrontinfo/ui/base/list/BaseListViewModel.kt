package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.MainThread
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel

// Stable (not Immutable): the ViewModel instance identity never changes and all UI-visible
// state lives in the observed StateFlows, so composition can safely skip when they are unchanged.
@Stable
abstract class BaseListViewModel : BaseViewModel() {

    // null = nothing loaded yet (cold start). A refresh deliberately keeps the previous list:
    // the UI never flashes empty mid-refresh, and in-place patches (updateModelDetail) stay
    // possible while the reload is still running.
    val modelsStateFlow: StateFlow<List<MyModel>?>
        field = MutableStateFlow<List<MyModel>?>(null)

    // Spinner flag, split from the data so loading no longer has to erase the list (the two
    // concerns replaced the former single State.Loading which carried no data).
    val isLoadingStateFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    abstract suspend fun collectModels(): List<MyModel>

    /**
     * Runs on the main thread right after each load's data lands. Override to reconcile state
     * that may have changed in the world while the list was being built — the freshly built
     * list can only embed the world as it was at some point mid-build (e.g. a Settings
     * switch toggled during a pull-to-refresh).
     */
    protected open fun onModelsLoaded() {}

    private var loadJob: Job? = null

    fun init() {
        // 1) In-memory cache present (config-change recreation)
        //    -> show it directly, never reload;
        // 2) Cold start / process death (app recycled in background, then resumed)
        //    -> no in-memory cache by definition (models == null)
        if (modelsStateFlow.value == null) {
            startLoad()
        }
    }

    fun refresh() {
        startLoad()
    }

    private fun startLoad() {
        if (loadJob?.isActive == true) {
            return
        }

        loadJob = viewModelScope.launch {
            setLoading(true)
            val list = collectModels()
            setModels(list)
            onModelsLoaded()
        }
    }

    @MainThread
    private fun setLoading(loading: Boolean) {
        isLoadingStateFlow.value = loading
    }

    @MainThread
    fun updateModelDetail(targetIndex: Int, newDetail: String) {
        modelsStateFlow.update { list ->
            if (list == null || targetIndex !in list.indices) {
                return@update list
            }

            val newList = list.toMutableList()
            newList[targetIndex] = newList[targetIndex].copy(detail = newDetail)
            newList
        }
    }

    @MainThread
    private fun setModels(tempModels: List<MyModel>) {
        modelsStateFlow.value = tempModels
        setLoading(false)
    }
}
