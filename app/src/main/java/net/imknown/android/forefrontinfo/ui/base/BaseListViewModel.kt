package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.LiveData
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
    protected val _models by lazy { MutableLiveData<ArrayList<MyModel>>() }
    val models: LiveData<ArrayList<MyModel>> by lazy { _models }

    private val _showModelsEvent by lazy { MutableLiveData<Event<Int>>() }
    val showModelsEvent: LiveData<Event<Int>> by lazy { _showModelsEvent }

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
            _showModelsEvent.value = Event(0)
        }
    }

    fun showError(error: Exception) = viewModelScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            _showErrorEvent.value = Event(error.message.toString())
        }

        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
    }

    @SuppressLint("PrivateApi")
    protected fun getStringProperty(key: String, condition: Boolean = true): String {
        return if (condition) {
            Class.forName("android.os.SystemProperties").getDeclaredMethod(
                "get",
                String::class.java,
                String::class.java
            ).invoke(null, key, MyApplication.getMyString(R.string.build_not_filled)) as String
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }
    }

//    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
//    protected fun setStringProperty(key: String, value: String) {
//        Class.forName("android.os.SystemProperties").getDeclaredMethod(
//            "set",
//            String::class.java,
//            String::class.java
//        ).invoke(null, key, value)
//    }

    protected fun shAsync(cmd: String, condition: Boolean = true): Deferred<List<String>> {
        return viewModelScope.async(Dispatchers.IO) {
            if (condition) {
                Shell.sh(cmd).exec().out
            } else {
                emptyList()
            }
        }
    }
}