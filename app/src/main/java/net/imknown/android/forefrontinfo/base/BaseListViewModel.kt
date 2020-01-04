package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

abstract class BaseListViewModel : ViewModel(), IAndroidVersion {
    val models by lazy { MutableLiveData<ArrayList<MyModel>>() }

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