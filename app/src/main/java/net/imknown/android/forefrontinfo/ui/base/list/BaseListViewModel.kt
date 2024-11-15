package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.BaseViewModel
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.stringEventLiveData
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.ShellResult

abstract class BaseListViewModel : BaseViewModel() {
    private val _models by lazy { MutableLiveData<ArrayList<MyModel>>() }
    val models: LiveData<ArrayList<MyModel>> by lazy { _models }

    private val _showModelsEvent by lazy { MutableLiveData<Event<Unit>>() }
    val showModelsEvent: LiveData<Event<Unit>> by lazy { _showModelsEvent }

    private val _showErrorEvent by lazy { MutableLiveData<Event<String>>() }
    val showErrorEvent: LiveData<Event<String>> by lazy { _showErrorEvent }

    private val _scrollBarMode by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }
    val scrollBarMode: LiveData<Event<String?>> by lazy { _scrollBarMode }

    abstract fun collectModels(): Job

    fun init(savedInstanceState: Bundle?) {
        viewModelScope.launch(Dispatchers.IO) {
            val scrollBarMode = MyApplication.sharedPreferences.getString(
                MyApplication.getMyString(R.string.interface_scroll_bar_key),
                MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
            )!!
            setScrollBarMode(scrollBarMode)

            // When activity is recreated, use LiveData to restore the data
            if (hasNoData(savedInstanceState)) {
                collectModels()
            }
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        (savedInstanceState == null || _showModelsEvent.value == null) && _showErrorEvent.value == null

    protected suspend fun setModels(tempModels: ArrayList<MyModel>) =
        withContext(Dispatchers.Main) {
            _models.value = tempModels
        }

    fun showModels(
        myModels: ArrayList<MyModel>,
        newModels: ArrayList<MyModel>
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (newModels.isEmpty()) {
            return@launch
        }

        myModels.clear()
        myModels.addAll(newModels)

        withContext(Dispatchers.Main) {
            _showModelsEvent.value = Event(Unit)
        }
    }

    protected fun showError(@StringRes messageId: Int, cause: Throwable) =
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _showErrorEvent.value = Event(MyApplication.getMyString(messageId, cause.message))
            }

            if (BuildConfig.DEBUG) {
                cause.printStackTrace()
            }
        }

    protected fun getStringProperty(key: String, condition: Boolean = true): String =
        if (condition) {
            PropertyManager.instance.getString(
                key, MyApplication.getMyString(R.string.build_not_filled)
            )
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }

    protected fun getBooleanProperty(key: String, condition: Boolean = true) =
        if (condition) {
            PropertyManager.instance.getBoolean(key, false)
        } else {
            false
        }

    protected fun sh(cmd: String, condition: Boolean = true): ShellResult {
        return if (condition) {
            ShellManager.instance.execute(cmd)
        } else {
            ShellResult()
        }
    }
}