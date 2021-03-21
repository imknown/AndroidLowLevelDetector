package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.base.Event
import net.imknown.android.forefrontinfo.ui.base.stringEventLiveData

abstract class BaseListViewModel : BaseViewModel() {
    private val _models by lazy { MutableLiveData<List<MyModel>>() }
    val models: LiveData<List<MyModel>> by lazy { _models }

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

    abstract fun collectModels()

    fun init(savedInstanceState: Bundle?) {
        val scrollBarMode = MyApplication.sharedPreferences.getString(
            MyApplication.getMyString(R.string.interface_scroll_bar_key), null
        ) ?: MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        setScrollBarMode(scrollBarMode)

        // When activity is recreated, use LiveData to restore the data
        if (hasNoData(savedInstanceState)) {
            collectModels()
        }
    }

    fun hasNoData(savedInstanceState: Bundle?) =
        (savedInstanceState == null || _showModelsEvent.value == null) && _showErrorEvent.value == null

    @MainThread
    protected fun setModels(tempModels: List<MyModel>) {
        _models.value = tempModels
    }

    @MainThread
    fun showModels(myModels: MutableList<MyModel>, newModels: List<MyModel>) {
        if (newModels.isEmpty()) {
            return
        }

        myModels.clear()
        myModels.addAll(newModels)

        _showModelsEvent.value = Event(Unit)
    }

    @MainThread
    protected fun showError(@StringRes messageId: Int, cause: Throwable) {
        _showErrorEvent.value = Event(MyApplication.getMyString(messageId, cause.fullMessage))

        if (BuildConfig.DEBUG) {
            cause.printStackTrace()
        }
    }
}