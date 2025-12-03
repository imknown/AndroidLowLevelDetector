package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.MainThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State

abstract class BaseListViewModel : BaseViewModel() {
    private val _modelsStateFlow by lazy { MutableStateFlow<State<List<MyModel>>>(State.NotInitialized) }
    val modelsStateFlow by lazy { _modelsStateFlow.asStateFlow() }

    abstract suspend fun collectModels(): List<MyModel>

    suspend fun init(savedInstanceState: Bundle?) {
        // When activity is recreated, use StateFlow to restore the data
        if (hasNoData(savedInstanceState)) {
            val list = collectModels()
            setModels(list)
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        savedInstanceState == null || _modelsStateFlow.value == State.NotInitialized

    @MainThread
    fun setModels(tempModels: List<MyModel>) {
        _modelsStateFlow.value = State.Done(tempModels)
    }
}