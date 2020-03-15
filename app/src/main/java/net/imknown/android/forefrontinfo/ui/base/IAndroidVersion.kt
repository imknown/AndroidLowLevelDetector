package net.imknown.android.forefrontinfo.ui.base

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.ui.home.model.Lld

interface IAndroidVersion {
    companion object {
        private const val CODENAME_RELEASE = "REL"

        private const val Build_VERSION_CODES_R = 30
    }

    private fun isAtLeastAndroid(version: Int, previewVersion: Int) =
        (Build.VERSION.SDK_INT >= version && Build.VERSION.CODENAME == CODENAME_RELEASE)
                || (Build.VERSION.SDK_INT >= previewVersion && Build.VERSION.CODENAME != CODENAME_RELEASE)

    fun isAndroid(version: Int, previewVersion: Int) =
        (Build.VERSION.SDK_INT == version && Build.VERSION.CODENAME == CODENAME_RELEASE)
                || (Build.VERSION.SDK_INT == previewVersion && Build.VERSION.CODENAME != CODENAME_RELEASE)

    fun isAtLeastAndroid6() =
        isAtLeastAndroid(Build.VERSION_CODES.M, Build.VERSION_CODES.LOLLIPOP_MR1)

    fun isAtLeastAndroid7() =
        isAtLeastAndroid(Build.VERSION_CODES.N, Build.VERSION_CODES.M)

    fun isAtLeastAndroid8() =
        isAtLeastAndroid(Build.VERSION_CODES.O, Build.VERSION_CODES.N_MR1)

    fun isAtLeastAndroid9() =
        isAtLeastAndroid(Build.VERSION_CODES.P, Build.VERSION_CODES.O_MR1)

    fun isAndroid10() = isAndroid(Build.VERSION_CODES.Q, Build.VERSION_CODES.P)

    fun isAtLeastAndroid10() =
        isAtLeastAndroid(Build.VERSION_CODES.Q, Build.VERSION_CODES.P)

    fun isAtLeastAndroid11() =
        isAtLeastAndroid(Build_VERSION_CODES_R, Build.VERSION_CODES.Q)

    @SuppressLint("ObsoleteSdkInt")
    fun isLatestStableAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.stable.api.toInt() && Build.VERSION.CODENAME == CODENAME_RELEASE

    fun isLatestPreviewAndroid(lld: Lld) =
        Build.VERSION.RELEASE >= lld.android.preview.name && Build.VERSION.CODENAME != CODENAME_RELEASE

    @SuppressLint("ObsoleteSdkInt")
    fun isSupportedByUpstreamAndroid(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

    // https://github.com/square/leakcanary/issues/1594
    // https://issuetracker.google.com/issues/139738913
    // https://github.com/pytorch/cpuinfo/blob/master/test/build.prop/huawei-p9-lite.log
    fun is10Leak() = isAndroid10()
            && (Build.ID.split('.').size < 2 || Build.ID.split('.')[1] < "191205")
}