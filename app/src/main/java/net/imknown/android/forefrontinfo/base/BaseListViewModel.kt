package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

abstract class BaseListViewModel : ViewModel(), IAndroidVersion {
    var models = MutableLiveData<ArrayList<MyModel>>()
        private set

    protected suspend fun sh(cmd: String, condition: Boolean = true): MutableList<String> =
        withContext(Dispatchers.IO) {
            if (condition) {
                Shell.sh(cmd).exec().out
            } else {
                emptyList()
            }
        }
}