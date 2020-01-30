package net.imknown.android.forefrontinfo.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event

abstract class BaseViewModel : ViewModel() {
    val changeScrollBarModeEvent by lazy { MutableLiveData<Event<Boolean>>() }

    fun setScrollBarMode(
        scrollBarMode: String
    ) = viewModelScope.launch(Dispatchers.Default) {
        when (scrollBarMode) {
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = Event(false)
                }
            }
            MyApplication.getMyString(R.string.interface_normal_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = Event(true)
                }
            }
            MyApplication.getMyString(R.string.interface_fast_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    changeScrollBarModeEvent.value = Event(false)
                }
            }
        }
    }
}