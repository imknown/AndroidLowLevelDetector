package net.imknown.android.forefrontinfo.base.mvvm

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.base.R

abstract class BaseViewModel : ViewModel() {
    private val _changeScrollBarModeEvent by lazy { MutableLiveData<Event<Boolean>>() }
    val changeScrollBarModeEvent: LiveData<Event<Boolean>> by lazy { _changeScrollBarModeEvent }

    fun setScrollBarMode(
        context: Context?,
        scrollBarMode: String
    ) = viewModelScope.launch(Dispatchers.Default) {
        when (scrollBarMode) {
            context?.getString(R.string.interface_no_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    _changeScrollBarModeEvent.value = Event(false)
                }
            }
            context?.getString(R.string.interface_normal_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    _changeScrollBarModeEvent.value = Event(true)
                }
            }
            context?.getString(R.string.interface_fast_scroll_bar_value) -> {
                withContext(Dispatchers.Main) {
                    _changeScrollBarModeEvent.value = Event(false)
                }
            }
        }
    }
}