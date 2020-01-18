package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.LanguageBroadcastLiveData
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.stringEventLiveData

abstract class BaseListViewModel : ViewModel(), IAndroidVersion {
    val models by lazy { MutableLiveData<ArrayList<MyModel>>() }

    val scrollBarMode by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }

    val language by lazy { LanguageBroadcastLiveData() }

    abstract suspend fun collectModels(): Job

    protected suspend fun sh(cmd: String, condition: Boolean = true): MutableList<String> =
        withContext(Dispatchers.IO) {
            if (condition) {
                Shell.sh(cmd).exec().out
            } else {
                emptyList()
            }
        }
}