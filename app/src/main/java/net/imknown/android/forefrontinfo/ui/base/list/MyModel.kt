package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes

data class MyModel(
    val title: String,
    var detail: String,
    @AttrRes val color: Int = 0
)