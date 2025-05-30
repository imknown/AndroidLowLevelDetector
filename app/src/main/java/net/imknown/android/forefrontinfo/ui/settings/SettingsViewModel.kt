package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository

@Stable
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

        var scrollBarModeChanged by mutableStateOf<String?>(null)
            private set

        var themeChanged by mutableStateOf<String?>(null)
            private set

        var outdatedOrderChangedCount by mutableIntStateOf(0)
            private set
    }

    fun emitScrollBarModeChanged(scrollBarMode: String?) {
        scrollBarModeChanged = scrollBarMode
    }

    fun emitThemeChanged(theme: String?) {
        themeChanged = theme
    }

    fun emitOutdatedOrderChanged() {
        outdatedOrderChangedCount++
    }

    // region [Version Info]
    var version by mutableStateOf<State<SettingsRepository.Version>>(State.NotInitialized)
        private set

    fun setBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ) {
        viewModelScope.launch {
            version = State.Done(settingsRepository.getBuiltInDataVersion(packageManager, packageName))
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
