package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
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

    suspend fun init(savedInstanceState: Bundle?) {
        // When activity is recreated, use StateFlow to restore the data
        if (hasNoData(savedInstanceState)) {
            val list = collectModels()
            setModels(list)
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        savedInstanceState == null || modelsStateFlow.value == State.NotInitialized

    fun refresh() {
        viewModelScope.launch {
            val list = collectModels()
            setModels(list)
        }
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