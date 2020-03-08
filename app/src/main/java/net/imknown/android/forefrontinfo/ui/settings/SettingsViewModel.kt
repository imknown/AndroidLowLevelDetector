package net.imknown.android.forefrontinfo.ui.settings

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event
import net.imknown.android.forefrontinfo.base.JsonIo
import net.imknown.android.forefrontinfo.base.stringEventLiveData
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.base.IAndroidVersion
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel : BaseViewModel(), IAndroidVersion {
    companion object {
        private const val ALGORITHM_SHA256 = "SHA-256"

        private const val KEY_SHA_256_GOOGLE =
            "A7:0C:41:07:C1:FD:F0:3E:9A:F9:C4:6F:4B:38:18:1C:04:D0:F6:46:DA:E6:09:8C:22:45:3D:9E:D2:69:72:6C"

        private const val KEY_SHA_256_IMKNOWN =
            "67:28:46:79:62:50:DF:A8:BE:10:1E:46:59:97:F8:94:0C:4F:FC:BC:B6:62:EB:86:23:BF:62:A6:D0:70:39:85"

        private const val KEY_SHA_256_PUBLIC_DEBUG =
            "F1:42:FD:28:A5:AD:78:D5:A6:F4:41:3B:00:B5:16:29:74:91:05:8F:B2:3B:2A:37:15:31:E7:75:63:76:6D:18"
    }

    private var counter = 5

    private val _version by lazy { MutableLiveData<Version>() }
    val version: LiveData<Version> by lazy { _version }

    private val _versionClick by lazy { MutableLiveData<Event<Int>>() }
    val versionClick: LiveData<Event<Int>> by lazy { _versionClick }

    val themesPrefChangeEvent by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_themes_key),
            MyApplication.getMyString(R.string.interface_themes_follow_system_value)
        )
    }

    val scrollBarModeChangeEvent by lazy {
        MyApplication.sharedPreferences.stringEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.interface_scroll_bar_key),
            MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
        )
    }

    fun setMyTheme(themesValue: Any) = viewModelScope.launch(Dispatchers.IO) {
        MyApplication.instance.setMyTheme(themesValue.toString())
    }

    fun setBuiltInDataVersion(
        packageName: String,
        packageManager: PackageManager
    ) = viewModelScope.launch(Dispatchers.IO) {
        // region [lld]
        val assetLldVersion = try {
            JsonIo.getAssetLldVersion(MyApplication.instance.assets)
        } catch (e: Exception) {
            e.printStackTrace()

            MyApplication.getMyString(android.R.string.unknownName)
        }
        // endregion [lld]

        // region [distributor]
        val mySha256 = getMyKeyPublicSha256(packageName, packageManager)
        val distributor = MyApplication.getMyString(getDistributor(mySha256))
        // endregion [distributor]

        // region [installer]
        val installerPackageName = getInstallerPackageName(packageName, packageManager)
        val installerLabel = installerPackageName?.let {
            getApplicationLabel(it, packageManager)
        }
        val installer = installerLabel?.let {
            "$it ($installerPackageName)"
        } ?: MyApplication.getMyString(R.string.about_installer_cl)
        // endregion [installer]

        // region [install time]
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val firstInstallTime = sdf.format(Date(packageInfo.firstInstallTime))
        val lastUpdateTime = sdf.format(Date(packageInfo.lastUpdateTime))
        // endregion [install time]

        withContext(Dispatchers.Main) {
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

    fun versionClicked() = viewModelScope.launch(Dispatchers.Default) {
        if (counter > 0) {
            counter -= 1
        } else if (counter == 0) {
            counter -= 100

            withContext(Dispatchers.Main) {
                _versionClick.value = Event(0)
            }
        }
    }

    private suspend fun getMyKeyPublicSha256(
        packageName: String,
        packageManager: PackageManager
    ) = withContext(Dispatchers.Default) {
        return@withContext if (isAtLeastAndroid9()) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )?.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )?.signatures
        }?.get(0)?.let {
            MessageDigest.getInstance(ALGORITHM_SHA256).digest(it.toByteArray())
                .joinToString(":") { byte -> "%02x".format(byte) }
                .toUpperCase(Locale.US)
        }
    }

    private suspend fun getDistributor(mySha256: String?) = withContext(Dispatchers.Default) {
        return@withContext when (mySha256) {
            KEY_SHA_256_GOOGLE -> R.string.about_distributor_google
            KEY_SHA_256_IMKNOWN -> R.string.about_distributor_imknown
            KEY_SHA_256_PUBLIC_DEBUG -> R.string.about_distributor_public_debug
            else -> android.R.string.unknownName
        }
    }

    private fun getInstallerPackageName(
        packageName: String,
        packageManager: PackageManager
    ) = packageManager.getInstallerPackageName(packageName)

    private fun getApplicationLabel(
        packageName: String,
        packageManager: PackageManager
    ): CharSequence {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        return packageManager.getApplicationLabel(applicationInfo) // applicationInfo.loadLabel(packageManager)
    }
}