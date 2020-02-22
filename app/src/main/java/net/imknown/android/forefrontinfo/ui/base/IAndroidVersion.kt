package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    fun isAtLeastAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    fun isAtLeastAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtLeastAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtLeastAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @SuppressLint("ObsoleteSdkInt")
    fun isLatestStableAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

    @SuppressLint("ObsoleteSdkInt")
    fun isLatestPreviewAndroid(lld: Lld) =
        Build.VERSION.RELEASE >= lld.android.preview.name

    @SuppressLint("ObsoleteSdkInt")
    fun isSupportedByUpstreamAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
}