package net.imknown.android.forefrontinfo.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GetRawPropEventViewModel : ViewModel() {
    var isGetPropFinish = MutableLiveData<Nothing?>()
        private set

    fun onFinish() {
        isGetPropFinish.value = null
    }
}
