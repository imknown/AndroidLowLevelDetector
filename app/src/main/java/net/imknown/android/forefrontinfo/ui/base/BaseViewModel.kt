package net.imknown.android.forefrontinfo.ui.base

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication

abstract class BaseViewModel : ViewModel() {
    private val _changeScrollBarModeEvent by lazy { MutableLiveData<Event<Boolean>>() }
    val changeScrollBarModeEvent: LiveData<Event<Boolean>> by lazy { _changeScrollBarModeEvent }

    @MainThread
    fun setScrollBarMode(scrollBarMode: String) {
        when (scrollBarMode) {
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value) -> {
                _changeScrollBarModeEvent.value = Event(false)
            }
            MyApplication.getMyString(R.string.interface_normal_scroll_bar_value) -> {
                _changeScrollBarModeEvent.value = Event(true)
            }
            MyApplication.getMyString(R.string.interface_fast_scroll_bar_value) -> {
                _changeScrollBarModeEvent.value = Event(false)
            }
        }
    }
}