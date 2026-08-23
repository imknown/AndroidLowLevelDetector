package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes
import androidx.annotation.StringRes

const val RES_ID_NONE = 0

enum class MyModelType {
    General,
    OutdatedTargetSdkApk
}

sealed interface MyModelTitle {
    data class Res(@StringRes val id: Int) : MyModelTitle
    data class Raw(val text: String) : MyModelTitle
}

data class MyModel(
    val title: MyModelTitle,
    val detail: String,
    @param:AttrRes val color: Int = RES_ID_NONE,
    val type: MyModelType = General
) {
    val key: String
        get() = when (title) {
            is MyModelTitle.Res -> title.id.toString()
            is MyModelTitle.Raw -> title.text
        }
}