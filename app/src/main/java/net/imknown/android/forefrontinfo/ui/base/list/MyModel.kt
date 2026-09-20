package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import net.imknown.android.forefrontinfo.ui.theme.StatusColor

enum class MyModelType {
    General,
    OutdatedTargetSdkApk
}

sealed interface MyModelTitle {
    data class Res(@StringRes val id: Int) : MyModelTitle
    data class Raw(val text: String) : MyModelTitle
}

@Immutable
data class MyModel(
    val title: MyModelTitle,
    val detail: String,
    val color: StatusColor = StatusColor.NONE,
    val type: MyModelType = General
) {
    val key: String
        get() = when (title) {
            is MyModelTitle.Res -> title.id.toString()
            is MyModelTitle.Raw -> title.text
        }
}