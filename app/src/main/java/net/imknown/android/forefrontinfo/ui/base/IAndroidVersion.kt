package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    companion object {
        private const val CODENAME_RELEASE = "REL"
    }

    fun isAtLeastStableAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    fun isAtLeastStableAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isAtLeastStableAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtLeastStableAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtLeastStableAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

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
}