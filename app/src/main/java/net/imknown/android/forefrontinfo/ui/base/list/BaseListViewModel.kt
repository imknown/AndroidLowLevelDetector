package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State

abstract class BaseListViewModel : BaseViewModel() {
    val modelsStateFlow: StateFlow<State<List<MyModel>>>
        field = MutableStateFlow<State<List<MyModel>>>(State.NotInitialized)

    abstract suspend fun collectModels(): List<MyModel>

    private var loadJob: Job? = null

    fun init() {
        // 1) In-memory cache present (config-change recreation)
        //    -> show it directly, never reload;
        // 2) Cold start / process death (app recycled in background, then resumed)
        //    -> no in-memory cache by definition (state == NotInitialized)
        if (modelsStateFlow.value == State.NotInitialized) {
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
            setLoading()
            val list = collectModels()
            setModels(list)
        }
    }

    @MainThread
    private fun setLoading() {
        modelsStateFlow.value = State.Loading
    }

    @MainThread
    fun updateModelDetail(targetIndex: Int, newDetail: String) {
        modelsStateFlow.update { state ->
            if (state !is State.Done) {
                return@update state
            }

            val list = state.value
            if (targetIndex !in list.indices) {
                return@update state
            }

            val newList = list.toMutableList()
            newList[targetIndex] = newList[targetIndex].copy(detail = newDetail)
            State.Done(newList)
        }
    }

    @MainThread
    private fun setModels(tempModels: List<MyModel>) {
        modelsStateFlow.value = State.Done(tempModels)
    }
}