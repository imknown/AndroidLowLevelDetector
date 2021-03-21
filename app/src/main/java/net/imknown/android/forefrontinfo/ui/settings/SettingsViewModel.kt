package net.imknown.android.forefrontinfo.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.mvvm.BaseViewModel
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.stringEventLiveData
import net.imknown.android.forefrontinfo.ui.common.LldManager
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

    private val _showMessageEvent by lazy { MutableLiveData<Event<Int>>() }
    val showMessageEvent: LiveData<Event<Int>> by lazy { _showMessageEvent }

    fun openInExternal(@StringRes uriResId: Int) = viewModelScope.launch {
        val resolved = withContext(Dispatchers.Default) {
            val uri = MyApplication.getMyString(uriResId).toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = intent.resolveActivity(MyApplication.instance.packageManager) != null
            if (resolved) {
                MyApplication.instance.startActivity(intent)
            }
            resolved
        }

        if (!resolved) {
            _showMessageEvent.value = Event(R.string.no_browser_found)
        }
    }

    // region [Version Info]
    private val _version by lazy { MutableLiveData<Version>() }
    val version: LiveData<Version> by lazy { _version }

    fun setBuiltInDataVersion(
        packageManager: PackageManager,
        packageName: String
    ) = viewModelScope.launch {
        // region [lld]
        val assetLldVersion = withContext(Dispatchers.IO) {
            LldManager.getAssetLldVersion(MyApplication.instance.assets)
                ?.formatToLocalZonedDatetimeString()
                ?: MyApplication.getMyString(android.R.string.unknownName)
        }
        // endregion [lld]

        // region [distributor]
        val distributor = withContext(Dispatchers.Default) {
            val mySha256 = settingsRepository.getPublicKeySha256OrThrow(packageManager, packageName)
            MyApplication.getMyString(settingsRepository.getDistributor(mySha256))
        }
        // endregion [distributor]

        // region [installer]
        val installer = withContext(Dispatchers.Default) {
            val installerPackageName =
                settingsRepository.getInstallerPackageNameOrNullOrThrow(packageManager, packageName)
            val installerLabel = installerPackageName?.let {
                try {
                    settingsRepository.getApplicationLabelOrThrow(packageManager, it)
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.d(javaClass.simpleName, "$packageName not found.")
                    MyApplication.getMyString(android.R.string.unknownName)
                }
            }
            installerLabel?.let {
                "$it ($installerPackageName)"
            } ?: MyApplication.getMyString(R.string.about_installer_cl)
        }
        // endregion [installer]

        // region [install time]
        val (firstInstallTime, lastUpdateTime) = withContext(Dispatchers.Default) {
            val packageInfo = settingsRepository.getPackageInfoOrThrow(packageManager, packageName)
            val firstInstallTime = packageInfo.firstInstallTime.formatToLocalZonedDatetimeString()
            val lastUpdateTime = packageInfo.lastUpdateTime.formatToLocalZonedDatetimeString()
            firstInstallTime to lastUpdateTime
        }
        // endregion [install time]

        _version.value = Version(
            R.string.about_version_summary,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            assetLldVersion,
            distributor,
            installer,
            firstInstallTime,
            lastUpdateTime
        )
    }

    data class Version(
        @StringRes val id: Int,
        val versionName: String,
        val versionCode: Int,
        val assetLldVersion: String,
        val distributor: String,
        val installer: String,
        val firstInstallTime: String,
        val lastUpdateTime: String
    )
    // endregion [Version Info]

    // region [Version Click]
    private var timesLeft = 7

    fun versionClicked() {
        if (timesLeft < 0) {
            return
        }

        if (--timesLeft == 0) {
            _showMessageEvent.value = Event(R.string.about_version_click)
        }
    }
    // endregion [Version Click]
}