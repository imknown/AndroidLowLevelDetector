package net.imknown.android.forefrontinfo.ui.settings.datasource

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid11
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid13

class AppInfoDataSource {
    fun getPackageInfoOrThrow(
        packageManager: PackageManager, packageName: String
    ): PackageInfo = if (isAtLeastAndroid13()) {
        val flags = PackageManager.PackageInfoFlags.of(0)
        packageManager.getPackageInfo(packageName, flags)
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }

    private fun getApplicationInfoOrThrow(
        packageManager: PackageManager, packageName: String
    ): ApplicationInfo = if (isAtLeastAndroid13()) {
        val flags = PackageManager.ApplicationInfoFlags.of(0)
        packageManager.getApplicationInfo(packageName, flags)
    } else {
        packageManager.getApplicationInfo(packageName, 0)
    }

    fun getInstallerPackageNameOrNullOrThrow(
        packageManager: PackageManager, packageName: String
    ): String? = if (isAtLeastAndroid11()) {
        packageManager.getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("Deprecation")
        packageManager.getInstallerPackageName(packageName)
    }

    fun getApplicationLabelOrThrow(
        packageManager: PackageManager, packageName: String
    ): CharSequence {
        val applicationInfo = getApplicationInfoOrThrow(packageManager, packageName)
        return packageManager.getApplicationLabel(applicationInfo) // applicationInfo.loadLabel(packageManager)
    }
}