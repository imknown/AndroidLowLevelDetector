package net.imknown.android.forefrontinfo.base

import android.annotation.SuppressLint
import android.os.Build
import androidx.fragment.app.Fragment
import net.imknown.android.forefrontinfo.ui.home.model.Lld

/**
 * https://en.wikipedia.org/wiki/Android_version_history
 * https://developer.android.com/about/dashboards/?hl=en
 * https://www.bidouille.org/misc/androidcharts
 *
 * https://www.google.com/android/beta?hl=zh-cn
 * https://developer.android.com/preview/overview?hl=zh-cn
 * https://ci.android.com/builds/branches/aosp-master/grid?
 * https://android.googlesource.com/platform/frameworks/base.git/+refs
 */
abstract class BaseFragment : Fragment() {
    protected fun isAtLeastAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    protected fun isAtLeastAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    protected fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    protected fun isAtLeastAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    protected fun isAtLeastAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @SuppressLint("ObsoleteSdkInt")
    protected fun isLatestStableAndroid(lld: Lld) =
        Build.VERSION.SDK_INT == lld.android.stable.api.toInt()

    @SuppressLint("ObsoleteSdkInt")
    protected fun isSupportedByUpstream(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
}