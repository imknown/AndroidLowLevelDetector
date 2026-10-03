package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository

// Stable (not Immutable): instance identity never changes and UI-visible state lives in the observed StateFlow;
// the two vars (load job, easter-egg counter) are never read for composition, so promising stability is safe (same as BaseListViewModel/HomeViewModel).
@Stable
// Metro wiring: the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

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