package net.imknown.android.forefrontinfo.ui.base

import android.app.ActivityManager
import android.os.Build
import androidx.core.content.ContextCompat
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    companion object {
        private const val CODENAME_RELEASE = "REL"
    }

    fun isAtLeastStableAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    fun isAtLeastStableAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isAtLeastStableAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtLeastStableAndroid8P1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    fun isAtLeastStableAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtLeastStableAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun isAtLeastStableAndroid11() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    fun isAtLeastStableAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
    fun isPreviewAndroid() = !isStableAndroid()

    fun isLatestStableAndroid(lld: Lld) = isStableAndroid()
            && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

    fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid()
            && getAndroidVersionName() >= lld.android.preview.name[0].toString()

    fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid()
            && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

    /**
     * For Android 11+, Use "Build.VERSION.RELEASE_OR_CODENAME" (ro.build.version.release_or_codename)
     */
    fun getAndroidVersionName(): String = if (isStableAndroid()) {
        Build.VERSION.RELEASE
    } else {
        Build.VERSION.CODENAME
    }

    fun isGoEdition() = isAtLeastStableAndroid8P1() && isLowRamDevice()

    private fun isLowRamDevice() = ContextCompat.getSystemService(
        MyApplication.instance, ActivityManager::class.java
    )?.isLowRamDevice == true

    fun getAndroidApiLevel() = if (isStableAndroid()) {
        Build.VERSION.SDK_INT
    } else {
        Build.VERSION_CODES.CUR_DEVELOPMENT
    }

//    fun getAndroidApiLevelDynamic() = MyApplication.instance.packageManager.getApplicationInfo(
//        "android", 0
//    ).targetSdkVersion
}