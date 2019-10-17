package net.imknown.android.forefrontinfo.ui.others

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OthersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is others Fragment"
    }
    val text: LiveData<String> = _text
}