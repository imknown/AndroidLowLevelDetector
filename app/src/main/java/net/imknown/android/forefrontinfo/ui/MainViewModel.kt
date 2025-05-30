package net.imknown.android.forefrontinfo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val SAVED_STATE_HANDLE_KEY_LAST_ID = "SAVED_STATE_HANDLE_KEY_LAST_ID"
    }

    var lastId = getSavedStateLastId()

    fun setSavedStateLastId(id: Int) {
        lastId = id
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id
    }

    private fun getSavedStateLastId() = savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID]
        ?: 0
}