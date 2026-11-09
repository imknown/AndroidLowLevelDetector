package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage

/*
 * Per-item failure isolation and row construction helpers, grouped by consumer
 * page (Others / Prop / shared).
 *
 * Guards wrap item producers: a failure degrades into that item's own error row
 * instead of escaping collectModels(); CancellationException always rethrows.
 * All guards are inline so the bare `return` statements in blocks keep working
 * as non-local returns, and the shared scaffold is the inline guardedItemCore.
 *
 * Dot page semantics (issues-cn #69): the red/yellow/green dot is a Home-only
 * per-item concept; Others / Prop / Settings never render dots. Home's own guard
 * lived here too (guardedMyModel, failure row CRITICAL for the red dot) until 2a
 * moved Home's failure isolation into HomeModelMapper's chained thin guard; what
 * remains here is the dotless pages' shape:
 * - guardedDetectFailedMyModel: colorless failure row, because dotless pages keep the
 *   plain row shape.
 */

// region [Others]

/**
 * Single-item guard, Others-only (the 36 detector methods in `OthersRepository`):
 * a failure degrades into that item's own colorless failure row, since Others has no
 * dots; the failure row keeps the plain row shape.
 */
inline fun guardedDetectFailedMyModel(@StringRes titleRes: Int, block: () -> MyModel): MyModel =
    guardedDetectFailedMyModel(MyModelTitle.Res(titleRes), block)

inline fun guardedDetectFailedMyModel(title: MyModelTitle, block: () -> MyModel): MyModel =
    guardedItemCore(block) { toDetectFailedMyModel(title, it) }

/**
 * Others' failure row: detail = "detection failed" text, no status color, used
 * by [guardedDetectFailedMyModel] and by `PropRepository.toDetectFailedMyModel`'s
 * delegation (Prop is dotless too, same row shape).
 */
@PublishedApi
internal fun toDetectFailedMyModel(title: MyModelTitle, e: Exception): MyModel = MyModel(
    title = title,
    detail = MyApplication.getMyString(R.string.result_detect_failed, e.fullMessage)
)

/**
 * Plain no-color row, Others-only (resource titles; the 36 regular data rows use
 * this overload). Prop does not use it, since Prop's titles are all key originals
 * returned by the system, handled by the String overload in the Prop region.
 */
fun toTranslatedDetailMyModel(@StringRes titleRes: Int, detail: String?): MyModel =
    MyModel(
        title = MyModelTitle.Res(titleRes),
        detail = toTranslatedDetail(detail)
    )

// endregion

// region [Prop]

/**
 * Plain no-color row, raw-text-title overload: every Prop data row (titles = key
 * originals returned by the system) uses it, as do Others' formatted-title rows
 * (the 2 partition fingerprint rows).
 */
fun toTranslatedDetailMyModel(title: String, detail: String?): MyModel =
    MyModel(
        title = MyModelTitle.Raw(title),
        detail = toTranslatedDetail(detail)
    )

/** Prop-only: parses one getprop output entry `"[key]: [value]"` into a row. */
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

// endregion

// region [Shared]

/**
 * The guards' shared scaffold: runs block, rethrows cancellation, and hands every
 * other exception to [onFailure] (after a debug print) to build the failure row.
 * Inline so non-local returns in caller blocks keep working.
 */
@PublishedApi
internal inline fun guardedItemCore(
    block: () -> MyModel,
    onFailure: (Exception) -> MyModel
): MyModel = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    if (BuildConfig.DEBUG) {
        e.printStackTrace()
    }
    onFailure(e)
}

private fun toTranslatedDetail(detail: String?): String = if (detail.isNullOrEmpty()) {
    MyApplication.getMyString(R.string.build_not_filled)
} else {
    detail
}

// endregion
