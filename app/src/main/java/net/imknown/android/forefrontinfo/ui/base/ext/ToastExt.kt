package net.imknown.android.forefrontinfo.ui.base.ext

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiContext

fun @receiver:UiContext Context.toast(@StringRes resId: Int) =
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
