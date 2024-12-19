package net.imknown.android.forefrontinfo.ui.settings.datasource

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.home.HomeViewModel.Companion.isAtLeastStableAndroid11
import net.imknown.android.forefrontinfo.ui.home.HomeViewModel.Companion.isAtLeastStableAndroid13

class AppInfoDataSource(private val dispatcher: CoroutineDispatcher) {
    suspend fun getPackageInfo(
        packageManager: PackageManager,
        packageName: String
    ) = withContext(dispatcher) {
        if (isAtLeastStableAndroid13()) {
            val flags = PackageManager.PackageInfoFlags.of(0)
            packageManager.getPackageInfo(packageName, flags)
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    suspend fun getInstallerPackageName(
        packageManager: PackageManager,
        packageName: String
    )  = withContext(dispatcher) {
        if (isAtLeastStableAndroid11()) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("Deprecation")
            packageManager.getInstallerPackageName(packageName)
        }
    }
}