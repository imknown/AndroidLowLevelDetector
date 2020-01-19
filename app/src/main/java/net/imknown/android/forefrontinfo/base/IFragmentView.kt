package net.imknown.android.forefrontinfo.base

import android.widget.Toast
import androidx.annotation.StringRes
import net.imknown.android.forefrontinfo.MyApplication

interface IFragmentView {

    fun toast(@StringRes resId: Int) =
        Toast.makeText(MyApplication.instance, resId, Toast.LENGTH_LONG).show()

    fun toast(text: String) =
        Toast.makeText(MyApplication.instance, text, Toast.LENGTH_LONG).show()
}