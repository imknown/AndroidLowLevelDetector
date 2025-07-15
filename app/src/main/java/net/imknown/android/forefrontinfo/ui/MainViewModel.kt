package net.imknown.android.forefrontinfo.ui

import androidx.annotation.IdRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import net.imknown.android.forefrontinfo.R

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val SAVED_STATE_HANDLE_KEY_LAST_ID = "SAVED_STATE_HANDLE_KEY_LAST_ID"
    }

    @IdRes
    var lastId = getSavedStateLastId()

    fun setSavedStateLastId(@IdRes id: Int) {
        lastId = id
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id
    }

    @IdRes
    private fun getSavedStateLastId() = savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID]
        ?: R.id.navigation_home
}