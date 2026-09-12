package net.imknown.android.forefrontinfo.ui

import androidx.annotation.IdRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.imknown.android.forefrontinfo.R

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val SAVED_STATE_HANDLE_KEY_LAST_ID = "SAVED_STATE_HANDLE_KEY_LAST_ID"
    }

    val lastId: StateFlow<Int> = savedStateHandle.getStateFlow(
        SAVED_STATE_HANDLE_KEY_LAST_ID, R.id.navigation_home
    )

    fun setSavedStateLastId(@IdRes id: Int) {
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id
    }
}