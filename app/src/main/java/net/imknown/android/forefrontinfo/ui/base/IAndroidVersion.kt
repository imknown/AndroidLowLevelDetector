package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    companion object {
        private const val CODENAME_RELEASE = "REL"
    }

    fun isAtLeastAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    fun isAtLeastAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtLeastAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtLeastAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isStableAndroid10() =
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && Build.VERSION.CODENAME == CODENAME_RELEASE

    @SuppressLint("ObsoleteSdkInt")
    fun isLatestStableAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

    fun isLatestPreviewAndroid(lld: Lld) =
        Build.VERSION.RELEASE >= lld.android.preview.name

    @SuppressLint("ObsoleteSdkInt")
    fun isSupportedByUpstreamAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

    // https://github.com/square/leakcanary/issues/1594
    // https://issuetracker.google.com/issues/139738913
    // https://github.com/pytorch/cpuinfo/blob/master/test/build.prop/huawei-p9-lite.log
    fun is10Leak() = isStableAndroid10()
            && (Build.ID.split('.').size < 2 || Build.ID.split('.')[1] < "191205")
}