package net.imknown.android.forefrontinfo.ui.base

import androidx.annotation.ColorRes
import net.imknown.android.forefrontinfo.R

data class MyModel(
    val title: String,
    var detail: String,
    @ColorRes val color: Int = R.color.colorStateless
)