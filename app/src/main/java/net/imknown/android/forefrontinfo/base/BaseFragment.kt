package net.imknown.android.forefrontinfo.base

import android.annotation.SuppressLint
import android.os.Build
import androidx.fragment.app.Fragment
import net.imknown.android.forefrontinfo.ui.home.model.Lld

abstract class BaseFragment : Fragment(), IFragmentView {
    @SuppressLint("ObsoleteSdkInt")
    protected fun isLatestStableAndroid(lld: Lld) =
        Build.VERSION.SDK_INT == lld.android.stable.api.toInt()

    @SuppressLint("ObsoleteSdkInt")
    protected fun isSupportedByUpstream(lld: Lld) =
        Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
}