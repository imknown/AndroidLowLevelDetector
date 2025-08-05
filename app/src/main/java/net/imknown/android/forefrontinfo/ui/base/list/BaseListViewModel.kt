package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State

abstract class BaseListViewModel : BaseViewModel() {
    private val _modelsStateFlow by lazy { MutableStateFlow<State<List<MyModel>>>(State.NotInitialized) }
    val modelsStateFlow by lazy { _modelsStateFlow.asStateFlow() }

    private val _showErrorEventStateFlow by lazy { MutableStateFlow<State<String>>(State.NotInitialized) }
    val showErrorEventStateFlow by lazy { _showErrorEventStateFlow.asStateFlow() }

    fun clearShowErrorEventStateFlow() {
        _showErrorEventStateFlow.value = State.NotInitialized
    }

    abstract fun collectModels()

    fun init(savedInstanceState: Bundle?) {
        // When activity is recreated, use StateFlow to restore the data
        if (hasNoData(savedInstanceState)) {
            collectModels()
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        savedInstanceState == null || _modelsStateFlow.value == State.NotInitialized

    @MainThread
    protected fun setModels(tempModels: List<MyModel>) {
        _modelsStateFlow.value = State.Done(tempModels)
    }

    @MainThread
    protected fun showError(@StringRes messageId: Int, cause: Throwable) {
        _showErrorEventStateFlow.value = State.Done(MyApplication.getMyString(messageId, cause.fullMessage))

        if (BuildConfig.DEBUG) {
            cause.printStackTrace()
        }
    }
}