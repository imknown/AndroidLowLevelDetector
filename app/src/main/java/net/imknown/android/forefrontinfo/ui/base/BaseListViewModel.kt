package net.imknown.android.forefrontinfo.ui.base

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event
import net.imknown.android.forefrontinfo.base.stringEventLiveData

abstract class BaseListViewModel : BaseViewModel(), IAndroidVersion {
    val models by lazy { MutableLiveData<ArrayList<MyModel>>() }

    val showModelsEvent by lazy { MutableLiveData<Event<Int>>() }
    val showErrorEvent by lazy { MutableLiveData<Event<String>>() }

    val scrollBarMode by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }

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
        savedInstanceState == null || models.value.isNullOrEmpty()

    fun showModels(
        myModels: ArrayList<MyModel>,
        newModels: ArrayList<MyModel>
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (newModels.isNullOrEmpty()) {
            return@launch
        }

        myModels.clear()
        myModels.addAll(newModels)

        withContext(Dispatchers.Main) {
            showModelsEvent.value = Event(0)
        }
    }

    fun showError(error: Exception) = viewModelScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            showErrorEvent.value = Event(error.message.toString())
        }

        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
    }

    protected fun shAsync(cmd: String, condition: Boolean = true): Deferred<MutableList<String>> {
        return viewModelScope.async(Dispatchers.IO) {
            if (condition) {
                Shell.sh(cmd).exec().out
            } else {
                emptyList()
            }
        }
    }
}