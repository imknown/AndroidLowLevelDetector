package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.MainThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State

abstract class BaseListViewModel : BaseViewModel() {
    val modelsStateFlow: StateFlow<State<List<MyModel>>>
        field = MutableStateFlow<State<List<MyModel>>>(State.NotInitialized)

    abstract suspend fun collectModels(): List<MyModel>

    suspend fun init() {
        val list = collectModels()
        setModels(list)
    }

    fun hasNoData() = modelsStateFlow.value == State.NotInitialized

    @MainThread
    fun setModels(tempModels: List<MyModel>) {
        modelsStateFlow.value = State.Done(tempModels)
    }
}