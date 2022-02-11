package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes
import com.google.android.material.R

data class MyModel(
    val title: String,
    var detail: String,
    @AttrRes val color: Int = R.attr.colorSurfaceVariant
)