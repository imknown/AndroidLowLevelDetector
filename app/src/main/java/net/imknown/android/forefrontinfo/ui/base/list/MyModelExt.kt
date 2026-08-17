package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.AttrRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication

fun toColoredMyModel(title: String, detail: String?, condition: Boolean): MyModel {
    @AttrRes val color = if (condition) R.attr.colorNoProblem else R.attr.colorCritical
    return MyModel(title, detail.toString(), color)
}

fun toColoredMyModel(title: String, detail: String?, @AttrRes color: Int): MyModel {
    return MyModel(title, detail.toString(), color)
}

fun toTranslatedDetailMyModel(title: String, detail: String?): MyModel {
    val translatedDetail = if (detail.isNullOrEmpty()) {
        MyApplication.getMyString(R.string.build_not_filled)
    } else {
        detail
    }
    return MyModel(title, translatedDetail)
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