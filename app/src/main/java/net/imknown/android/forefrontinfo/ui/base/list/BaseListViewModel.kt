package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel

abstract class BaseListViewModel : BaseViewModel() {
    private val _modelsStateFlow by lazy { MutableStateFlow<List<MyModel>?>(null) }
    val modelsStateFlow: StateFlow<List<MyModel>?> by lazy { _modelsStateFlow.asStateFlow() }

    private val _showModelsEventStateFlow by lazy { MutableStateFlow<Unit?>(null) }
    val showModelsEventStateFlow: StateFlow<Unit?> by lazy { _showModelsEventStateFlow.asStateFlow() }

    private val _showErrorEventStateFlow by lazy { MutableStateFlow<String?>(null) }
    val showErrorEventStateFlow: StateFlow<String?> by lazy { _showErrorEventStateFlow.asStateFlow() }

    fun clearShowModelsEventStateFlow() {
        _showModelsEventStateFlow.value = null
    }

    fun clearShowErrorEventStateFlow() {
        _showErrorEventStateFlow.value = null
    }

    abstract fun collectModels()

    fun init(savedInstanceState: Bundle?) {
        // When activity is recreated, use StateFlow to restore the data
        if (hasNoData(savedInstanceState)) {
            collectModels()
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        (savedInstanceState == null || _showModelsEventStateFlow.value == null) && _showErrorEventStateFlow.value == null

    @MainThread
    protected fun setModels(tempModels: List<MyModel>) {
        _modelsStateFlow.value = tempModels
    }

    @MainThread
    fun showModels(myModels: MutableList<MyModel>, newModels: List<MyModel>) {
        if (newModels.isEmpty()) {
            return
        }

        myModels.clear()
        myModels.addAll(newModels)

        _showModelsEventStateFlow.value = Unit
    }

    @MainThread
    protected fun showError(@StringRes messageId: Int, cause: Throwable) {
        _showErrorEventStateFlow.value = MyApplication.getMyString(messageId, cause.fullMessage)

        if (BuildConfig.DEBUG) {
            cause.printStackTrace()
        }
    }
}