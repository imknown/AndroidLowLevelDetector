package net.imknown.android.forefrontinfo.base

import android.os.Build
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import net.imknown.android.forefrontinfo.R

abstract class BaseFragment : Fragment() {
    protected fun isAtLeastAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    protected fun isAtLeastAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    protected fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    protected fun isAtLeastAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    protected fun isAtLeastAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    protected fun getResultString(@StringRes stringId: Int, vararg value: Any?): String {
        return if (value.isNullOrEmpty()) {
            getString(stringId, getString(R.string.build_not_filled))
        } else {
            getString(stringId, *value)
        }
    }
}