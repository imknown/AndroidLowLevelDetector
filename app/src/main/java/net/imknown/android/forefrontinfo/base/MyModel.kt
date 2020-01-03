package net.imknown.android.forefrontinfo.base

import androidx.annotation.ColorInt
import net.imknown.android.forefrontinfo.MainActivity

data class MyModel(
    val title: String,
    val detail: String,
    @ColorInt val color: Int = MainActivity.COLOR_STATE_LIST_DEFAULT_STYLE
)