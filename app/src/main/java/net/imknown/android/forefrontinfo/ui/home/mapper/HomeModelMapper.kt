package net.imknown.android.forefrontinfo.ui.home.mapper

import androidx.annotation.StringRes
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.theme.StatusColor

/**
 * The single translation point from domain objects to [MyModel] (issues-cn #01): the repository
 * states facts only, and display text and colors are decided here. Colors have no cross-entry
 * mapping (NFR-7); each entry picks its own semantics, so the translation methods land here,
 * one per entry.
 */
@Inject
class HomeModelMapper(private val myApplication: MyApplication) {

    /**
     * The developer options row: red while enabled, green while disabled (this setting's semantics
     * is "enabled is the risk").
     *
     * This method is also the batch's UI-side thin guard: it wraps the whole fetch-and-translate
     * chain and degrades unexpected exceptions into this entry's own failure row, so one failing
     * entry cannot take the whole page down (the same coverage the old guardedMyModel had).
     * [CancellationException] always rethrows: a cancelled coroutine must not become a detection
     * failure. The failure DEBUG print lives in [failureRow], so both failure paths are covered.
     * When the engine lands, this layer moves into it and the wrapping goes away.
     *
     * The Missing branch is unreachable for this entry (an unreadable value surfaces as Failed);
     * it exists for when exhaustiveness and renders the unknown row with reason as its detail.
     * The reason's shape is a later batch's call (spec finding 3).
     */
    fun toDeveloperOptionsModel(detect: () -> DetectResult<Boolean>): MyModel = try {
        when (val result = detect()) {
            is DetectResult.Found -> coloredRow(
                R.string.developer_options_status_title,
                myApplication.getString(
                    if (result.value) R.string.result_disabled else R.string.result_enabled
                ),
                result.value
            )
            is DetectResult.Missing -> unknownRow(R.string.developer_options_status_title, result.reason)
            is DetectResult.Failed -> failureRow(R.string.developer_options_status_title, result.cause)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failureRow(R.string.developer_options_status_title, e)
    }

    // Two-state row color mapping: a true condition lands on NO_PROBLEM. Home-only, dots are a
    // Home-only concept (#69).
    private fun coloredRow(@StringRes titleRes: Int, detail: String, condition: Boolean): MyModel =
        MyModel(
            title = MyModelTitle.Res(titleRes),
            detail = detail,
            color = if (condition) StatusColor.NO_PROBLEM else StatusColor.CRITICAL
        )

    // Unknown row: Missing lands on unknown, same color as the failure row (the red dot is an
    // accepted trade-off, issues-cn #33).
    private fun unknownRow(@StringRes titleRes: Int, reason: String): MyModel =
        MyModel(
            title = MyModelTitle.Res(titleRes),
            detail = reason,
            color = StatusColor.CRITICAL
        )

    // Failure row: the "detection failed" text carries the exception's full message, matching
    // guardedMyModel's failure rendering. Both failure paths (a Failed handed over by the
    // repository and an unexpected in-chain exception) meet here, so the print lives only in this
    // one place: one print per failure, a stack trace to investigate in DEBUG builds
    // (FR-3 AC2: details go to the log).
    private fun failureRow(@StringRes titleRes: Int, e: Exception): MyModel {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
        return MyModel(
            title = MyModelTitle.Res(titleRes),
            detail = myApplication.getString(R.string.result_detect_failed, e.fullMessage),
            color = StatusColor.CRITICAL
        )
    }
}
