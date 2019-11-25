package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val isSucceed = MutableLiveData<Boolean>()

    fun onGetPropFinish(value: Boolean) {
        isSucceed.value = value
    }
}
