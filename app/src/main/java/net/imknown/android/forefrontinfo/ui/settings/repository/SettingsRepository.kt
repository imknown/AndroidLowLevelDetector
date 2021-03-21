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
import net.imknown.android.forefrontinfo.ui.base.IRepository
import net.imknown.android.forefrontinfo.ui.common.LldManager
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource

class SettingsRepository(
    private val appInfoDataSource: AppInfoDataSource,
    private val fingerprintDataSource: FingerprintDataSource
) : IRepository {
    suspend fun getBuiltInDataVersion(
        packageManager: PackageManager, packageName: String
    ): Version {
        // region [lld]
        val assetLldVersion = withContext(Dispatchers.IO) {
            LldManager.getAssetLldVersion(MyApplication.instance.assets)
                ?.formatToLocalZonedDatetimeString()
                ?: MyApplication.getMyString(android.R.string.unknownName)
        }
        // endregion [lld]

        // region [distributor]
        val distributor = withContext(Dispatchers.Default) {
            val mySha256 = fingerprintDataSource.getPublicKeySha256OrThrow(packageManager, packageName)
            val distributorResId = fingerprintDataSource.getDistributor(mySha256)
            MyApplication.getMyString(distributorResId)
        }
        // endregion [distributor]

        // region [installer]
        val installer = withContext(Dispatchers.Default) {
            val installerPackageName =
                appInfoDataSource.getInstallerPackageNameOrNullOrThrow(packageManager, packageName)
            val installerLabel = installerPackageName?.let {
                try {
                    appInfoDataSource.getApplicationLabelOrThrow(packageManager, packageName)
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
            val packageInfo = appInfoDataSource.getPackageInfoOrThrow(packageManager, packageName)
            val firstInstallTime = packageInfo.firstInstallTime.formatToLocalZonedDatetimeString()
            val lastUpdateTime = packageInfo.lastUpdateTime.formatToLocalZonedDatetimeString()
            firstInstallTime to lastUpdateTime
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
        @StringRes val id: Int,
        val versionName: String,
        val versionCode: Int,
        val assetLldVersion: String,
        val distributor: String,
        val installer: String,
        val firstInstallTime: String,
        val lastUpdateTime: String
    )
}