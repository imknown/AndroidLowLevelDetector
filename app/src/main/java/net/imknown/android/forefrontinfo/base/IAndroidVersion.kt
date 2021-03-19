package net.imknown.android.forefrontinfo.base

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import net.imknown.android.forefrontinfo.MyApplication
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

    fun isGoEdition() = isAtLeastStableAndroid8P1()
            && (MyApplication.instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice

    fun getAndroidApiLevel() = if (isStableAndroid()) {
        Build.VERSION.SDK_INT
    } else {
        Build.VERSION_CODES.CUR_DEVELOPMENT
    }

//    fun getAndroidApiLevelDynamic() = MyApplication.instance.packageManager.getApplicationInfo(
//        "android", 0
//    ).targetSdkVersion
}