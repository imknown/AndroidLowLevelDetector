package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    companion object {
        private const val CODENAME_RELEASE = "REL"

        // https://cs.android.com/android/platform/superproject/+/master:libcore/libart/src/main/java/dalvik/system/VMRuntime.java?q=SDK_VERSION_CUR_DEVELOPMENT
        private const val SDK_VERSION_CUR_DEVELOPMENT = 10000
    }

    fun isAtLeastStableAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    fun isAtLeastStableAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isAtLeastStableAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtLeastStableAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtLeastStableAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun isAtLeastStableAndroid11() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
    fun isPreviewAndroid() = !isStableAndroid()

    @SuppressLint("ObsoleteSdkInt")
    fun isLatestStableAndroid(lld: Lld) = isStableAndroid()
            && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

    fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid()
            && getAndroidVersionName() >= lld.android.preview.name

    @SuppressLint("ObsoleteSdkInt")
    fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid()
            && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

    /**
     * TODO: For Android 11+, Use "Build.VERSION.RELEASE_OR_CODENAME" (ro.build.version.release_or_codename)
     */
    fun getAndroidVersionName(): String = if (isStableAndroid()) {
        Build.VERSION.RELEASE
    } else {
        Build.VERSION.CODENAME
    }

    fun getAndroidApiLevel() = if (isStableAndroid()) {
        Build.VERSION.SDK_INT
    } else {
        SDK_VERSION_CUR_DEVELOPMENT
    }

    fun getAndroidApiLevelDynamic() = MyApplication.instance.packageManager.getApplicationInfo(
        "android", 0
    ).targetSdkVersion
}