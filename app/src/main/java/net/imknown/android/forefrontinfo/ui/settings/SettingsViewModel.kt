package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        val MY_REPOSITORY_KEY = object : CreationExtras.Key<SettingsRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as SettingsRepository
                val savedStateHandle = createSavedStateHandle()
                SettingsViewModel(repository, savedStateHandle)
            }
        }

        private val _scrollBarModeChangedSharedFlow = MutableSharedFlow<String?>()
        val scrollBarModeChangedSharedFlow = _scrollBarModeChangedSharedFlow.asSharedFlow()

        private val _outdatedOrderChangedSharedFlow = MutableSharedFlow<Unit>()
        val outdatedOrderChangedSharedFlow = _outdatedOrderChangedSharedFlow.asSharedFlow()
    }

    fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {
        viewModelScope.launch {
            _scrollBarModeChangedSharedFlow.emit(scrollBarMode)
        }
    }

    fun emitOutdatedOrderChangedSharedFlow() {
        viewModelScope.launch {
            _outdatedOrderChangedSharedFlow.emit(Unit)
        }
    }

    // region [Version Info]
    private val _version by lazy { MutableStateFlow<State<SettingsRepository.Version>>(State.NotInitialized) }
    val version by lazy { _version.asStateFlow() }

    fun setBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ) {
        viewModelScope.launch {
            _version.value = State.Done(settingsRepository.getBuiltInDataVersion(packageManager, packageName))
        }
    }
    // endregion [Version Info]

    // region [Version Click]
    private var timesLeft = 7

    @StringRes
    fun getVersionClickedMessage(): Int? {
        if (timesLeft <= 0) {
            return null
        }

        timesLeft--

        if (timesLeft > 0) {
            return null
        }

        return R.string.about_version_click
    }
    // endregion [Version Click]
}