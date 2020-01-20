package net.imknown.android.forefrontinfo.base

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.*

abstract class BaseListViewModel : BaseViewModel(), IAndroidVersion {
    val models by lazy { MutableLiveData<ArrayList<MyModel>>() }

    val showModelsEvent by lazy { MutableLiveData<SingleEvent<Int>>() }
    val showErrorEvent by lazy { MutableLiveData<SingleEvent<String>>() }

    val scrollBarMode by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }

    val language by lazy { LanguageBroadcastLiveData() }

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
        // TODO: Maybe leak, consider moving these logic to the Adapter's ViewModel
        myAdapter: MyAdapter,
        myModels: ArrayList<MyModel>
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (myModels.isNullOrEmpty()) {
            return@launch
        }

        myAdapter.addAll(myModels)

        withContext(Dispatchers.Main) {
            showModelsEvent.value = SingleEvent(0)
        }
    }

    fun showError(error: Exception) = viewModelScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            showErrorEvent.value = SingleEvent(error.message.toString())
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