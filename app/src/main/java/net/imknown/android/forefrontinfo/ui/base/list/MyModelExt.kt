package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.theme.StatusColor

/**
 * Per-item error isolation: every item producer runs inside one of these guards, so a failing
 * probe degrades into its own row (same title, the error text as detail) instead of escaping
 * `collectModels()` and crashing the load. Cancellation still propagates. The guards are
 * inline so the original bodies' plain `return` statements keep working as non-local returns
 * (finally still runs on the way out).
 */
inline fun guardedMyModel(@StringRes titleRes: Int, block: () -> MyModel): MyModel =
    guardedMyModel(MyModelTitle.Res(titleRes), block)

inline fun guardedMyModel(title: MyModelTitle, block: () -> MyModel): MyModel =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
        toErrorMyModel(title, e)
    }

inline fun guardedMyModels(@StringRes titleRes: Int, block: () -> List<MyModel>): List<MyModel> =
    guardedMyModels(MyModelTitle.Res(titleRes), block)

inline fun guardedMyModels(title: MyModelTitle, block: () -> List<MyModel>): List<MyModel> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
        listOf(toErrorMyModel(title, e))
    }

@PublishedApi
internal fun toErrorMyModel(title: MyModelTitle, e: Exception) = MyModel(
    title = title,
    detail = MyApplication.getMyString(R.string.result_detect_failed, e.fullMessage),
    color = StatusColor.CRITICAL
)

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
