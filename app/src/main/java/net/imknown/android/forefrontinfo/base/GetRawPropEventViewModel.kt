package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GetRawPropEventViewModel : ViewModel() {
    val isGetPropFinish = MutableLiveData<Nothing?>()

    fun onFinish() {
        isGetPropFinish.value = null
    }
}
