package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes

const val RES_ID_NONE = 0

enum class MyModelType {
    General,
    OutdatedTargetSdkApk
}

data class MyModel(
    val title: String,
    val detail: String,
    @param:AttrRes val color: Int = RES_ID_NONE,
    val type: MyModelType = General
)