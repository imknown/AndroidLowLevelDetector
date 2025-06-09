package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.base.Event
import net.imknown.android.forefrontinfo.ui.base.stringEventLiveData
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
    }

    private val _themesPrefChangeEvent by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_themes_key),
            MyApplication.getMyString(R.string.interface_themes_follow_system_value)
        )
    }
    val themesPrefChangeEvent: LiveData<Event<String?>> by lazy { _themesPrefChangeEvent }

    private val _scrollBarModeChangedEvent by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }
    val scrollBarModeChangedEvent: LiveData<Event<String?>> by lazy { _scrollBarModeChangedEvent }

    // region [Version Info]
    private val _version by lazy { MutableLiveData<SettingsRepository.Version>() }
    val version: LiveData<SettingsRepository.Version> by lazy { _version }

    fun setBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ) = viewModelScope.launch {
        _version.value = settingsRepository.getBuiltInDataVersion(packageManager, packageName)
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