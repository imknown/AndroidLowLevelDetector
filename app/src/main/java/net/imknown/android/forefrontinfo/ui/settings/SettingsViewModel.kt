package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository

// Stable (not Immutable): instance identity never changes and UI-visible state lives in the observed StateFlow;
// the two vars (load job, easter-egg counter) are never read for composition, so promising stability is safe (same as BaseListViewModel/HomeViewModel).
@Stable
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(SettingsRepository(AppInfoDataSource(), FingerprintDataSource()))
            }
        }

        val scrollBarModeChangedSharedFlow: SharedFlow<String?>
            field = MutableSharedFlow()
    }

    fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {
        viewModelScope.launch {
            scrollBarModeChangedSharedFlow.emit(scrollBarMode)
        }
    }

    // region [Version Info]
    // null = not loaded yet (the built-in data version loads once; there is no reload, so no
    // State wrapper is needed — the former State.Loading branch was never used here)
    val version: StateFlow<SettingsRepository.Version?>
        field = MutableStateFlow<SettingsRepository.Version?>(null)

    private var initBuiltInDataVersionJob: Job? = null

    fun setBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ) {
        if (version.value != null
            || initBuiltInDataVersionJob?.isActive == true
        ) {
            return
        }

        initBuiltInDataVersionJob = viewModelScope.launch {
            version.value = settingsRepository.getBuiltInDataVersion(packageManager, packageName)
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