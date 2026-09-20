package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.StringRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.theme.StatusColor

fun toColoredMyModel(@StringRes titleRes: Int, detail: String?, condition: Boolean): MyModel {
    val color = if (condition) StatusColor.NO_PROBLEM else StatusColor.CRITICAL
    return MyModel(
        title = MyModelTitle.Res(titleRes),
        detail = detail.toString(),
        color = color
    )
}

fun toColoredMyModel(@StringRes titleRes: Int, detail: String?, color: StatusColor): MyModel {
    return MyModel(
        title = MyModelTitle.Res(titleRes),
        detail = detail.toString(),
        color = color
    )
}

fun toTranslatedDetailMyModel(@StringRes titleRes: Int, detail: String?): MyModel =
    MyModel(
        title = MyModelTitle.Res(titleRes),
        detail = toTranslatedDetail(detail)
    )

fun toTranslatedDetailMyModel(title: String, detail: String?): MyModel =
    MyModel(
        title = MyModelTitle.Raw(title),
        detail = toTranslatedDetail(detail)
    )

private fun toTranslatedDetail(detail: String?): String = if (detail.isNullOrEmpty()) {
    MyApplication.getMyString(R.string.build_not_filled)
} else {
    detail
}

fun toPropMyModel(rawProp: String): MyModel {
    val result = rawProp.split(": ")
    val title = removeSquareBrackets(result[0])
    val detail = result.getOrNull(1)?.let {
        removeSquareBrackets(it)
    }
    return toTranslatedDetailMyModel(title, detail)
}

private fun removeSquareBrackets(text: String): String =
    text.substringAfter("[").substringBefore(']').trimIndent()
