package net.imknown.android.forefrontinfo.ui.settings.repository

import android.content.pm.PackageManager
import net.imknown.android.forefrontinfo.ui.base.IRepository
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource

class SettingsRepository(
    private val appInfoDataSource: AppInfoDataSource,
    private val fingerprintDataSource: FingerprintDataSource
) : IRepository {
    fun getPackageInfoOrThrow(packageManager: PackageManager, packageName: String) =
        appInfoDataSource.getPackageInfoOrThrow(packageManager, packageName)

    fun getInstallerPackageNameOrNullOrThrow(packageManager: PackageManager, packageName: String) =
        appInfoDataSource.getInstallerPackageNameOrNullOrThrow(packageManager, packageName)

    fun getApplicationLabelOrThrow(packageManager: PackageManager, packageName: String) =
        appInfoDataSource.getApplicationLabelOrThrow(packageManager, packageName)

    fun getPublicKeySha256OrThrow(packageManager: PackageManager, packageName: String) =
        fingerprintDataSource.getPublicKeySha256OrThrow(packageManager, packageName)

    fun getDistributor(mySha256: String?) =
        fingerprintDataSource.getDistributor(mySha256)
}