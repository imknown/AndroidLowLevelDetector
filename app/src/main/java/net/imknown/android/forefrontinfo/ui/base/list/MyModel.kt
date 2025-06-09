package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes

const val RES_ID_NONE = 0

class MyModel(
    val title: String,
    var detail: String,
    @param:AttrRes val color: Int = RES_ID_NONE
)