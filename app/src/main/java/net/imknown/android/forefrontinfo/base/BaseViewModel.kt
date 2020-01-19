package net.imknown.android.forefrontinfo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

abstract class BaseViewModel : ViewModel() {
    val changeScrollBarModeEvent by lazy { MutableLiveData<SingleEvent<Boolean>>() }

    fun setScrollBarMode(
        scrollBarMode: String
    ) = viewModelScope.launch(Dispatchers.Default) {
        when (scrollBarMode) {
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = SingleEvent(false)
                }
            }
            MyApplication.getMyString(R.string.interface_normal_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = SingleEvent(true)
                }
            }
            MyApplication.getMyString(R.string.interface_fast_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = SingleEvent(false)
                }
            }
        }
    }
}