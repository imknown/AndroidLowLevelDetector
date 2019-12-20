package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val isGetPropFinish = MutableLiveData<Boolean>()

    fun onGetPropFinish() {
        isGetPropFinish.value = true
    }
}
