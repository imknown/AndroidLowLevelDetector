package net.imknown.android.forefrontinfo.ui.settings.repository

import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.common.LldManager
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource
import android.R as androidR

class SettingsRepository(
    private val appInfoDataSource: AppInfoDataSource,
    private val fingerprintDataSource: FingerprintDataSource
) {
    suspend fun getBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ): Version {
        // region [lld]
        val assetLldVersion = withContext(Dispatchers.IO) {
            LldManager.getAssetLldVersion(MyApplication.instance.assets)
                ?.formatToLocalZonedDatetimeString()
                ?: MyApplication.getMyString(androidR.string.unknownName)
        }
        // endregion [lld]

        // region [distributor]
        val distributor = withContext(Dispatchers.Default) {
            val mySha256 = try {
                fingerprintDataSource.getPublicKeySha256OrNullOrThrow(packageManager, packageName)
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "$packageName PublicKey SHA-256 not found. ${e.fullMessage}")
                null
            }
            val distributorResId = fingerprintDataSource.getDistributor(mySha256)
            MyApplication.getMyString(distributorResId)
        }
        // endregion [distributor]

        // region [installer]
        val installer = withContext(Dispatchers.Default) {
            val installerPackageName = try {
                appInfoDataSource.getInstallerPackageNameOrNullOrThrow(packageManager, packageName)
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "$packageName Installer not found. ${e.fullMessage}")
                null
            }
            val installerLabel = installerPackageName?.let {
                try {
                    appInfoDataSource.getApplicationLabelOrThrow(packageManager, packageName)
                } catch (e: Exception) {
                    Log.w(javaClass.simpleName, "$packageName not found. ${e.fullMessage}")
                    MyApplication.getMyString(androidR.string.unknownName)
                }
            }
            installerLabel?.let {
                "$it ($installerPackageName)"
            } ?: MyApplication.getMyString(R.string.about_installer_cl)
        }
        // endregion [installer]

        // region [install time]
        val (firstInstallTime, lastUpdateTime) = withContext(Dispatchers.Default) {
            val packageInfo = try {
                appInfoDataSource.getPackageInfoOrThrow(packageManager, packageName)
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "$packageName PackageInfo not found. ${e.fullMessage}")
                null
            }
            if (packageInfo != null) {
                val firstInstallTime = packageInfo.firstInstallTime.formatToLocalZonedDatetimeString()
                val lastUpdateTime = packageInfo.lastUpdateTime.formatToLocalZonedDatetimeString()
                firstInstallTime to lastUpdateTime
            } else {
                val unknown = MyApplication.getMyString(androidR.string.unknownName)
                unknown to unknown
            }
        }
        // endregion [install time]

        return Version(
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
        @param:StringRes val id: Int,
        val versionName: String,
        val versionCode: Int,
        val assetLldVersion: String,
        val distributor: String,
        val installer: String,
        val firstInstallTime: String,
        val lastUpdateTime: String
    )
}