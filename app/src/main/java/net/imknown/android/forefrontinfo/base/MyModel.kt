package net.imknown.android.forefrontinfo.base

import androidx.annotation.ColorRes
import net.imknown.android.forefrontinfo.R

data class MyModel(
    val title: String,
    val detail: String,
    @ColorRes val color: Int = R.color.colorStateless
)