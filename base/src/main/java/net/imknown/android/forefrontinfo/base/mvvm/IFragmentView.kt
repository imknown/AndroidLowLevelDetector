package net.imknown.android.forefrontinfo.base.mvvm

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

interface IFragmentView : IView {
    val visualContext: Context?

    fun toast(@StringRes resId: Int) = visualContext?.let {
        Toast.makeText(it, resId, Toast.LENGTH_LONG).show()
    }

    fun toast(text: String) = visualContext?.let {
        Toast.makeText(it, text, Toast.LENGTH_LONG).show()
    }
}