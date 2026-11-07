package net.imknown.android.forefrontinfo.ui.home.mapper

import androidx.annotation.StringRes
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.home.model.AbState
import net.imknown.android.forefrontinfo.ui.home.model.AndroidInfoState
import net.imknown.android.forefrontinfo.ui.home.model.DynamicPartitionsState
import net.imknown.android.forefrontinfo.ui.home.model.KernelState
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import net.imknown.android.forefrontinfo.ui.home.model.LldStatusState
import net.imknown.android.forefrontinfo.ui.home.model.PerformanceClassInfo
import net.imknown.android.forefrontinfo.ui.home.model.SdkExtensionState
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import android.R as androidR

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
     * is "enabled is the risk"). Runs through the shared [guardedEntry] scaffold; the Missing
     * branch is unreachable for this entry (an unreadable value surfaces as Failed) and renders
     * the unknown row via the scaffold's default. The reason's shape is a later batch's call
     * (spec finding 3).
     */
    fun toDeveloperOptionsModel(detect: () -> DetectResult<Boolean>): MyModel = guardedEntry(
        R.string.developer_options_status_title,
        detect
    ) { isDisabled ->
        coloredRow(
            R.string.developer_options_status_title,
            myApplication.getString(
                if (isDisabled) R.string.result_disabled else R.string.result_enabled
            ),
            isDisabled
        )
    }

    // The developer options row's twin: the same Settings read shape, semantics aligned
    // (disabled is the good state).
    fun toAdbModel(detect: () -> DetectResult<Boolean>): MyModel = guardedEntry(
        R.string.adb_debugging_status_title,
        detect
    ) { isDisabled ->
        coloredRow(
            R.string.adb_debugging_status_title,
            myApplication.getString(
                if (isDisabled) R.string.result_disabled else R.string.result_enabled
            ),
            isDisabled
        )
    }

    // The ADB authentication row: enabled is the good state, the opposite direction of the
    // two rows above.
    fun toAdbAuthenticationModel(detect: () -> DetectResult<Boolean>): MyModel = guardedEntry(
        R.string.adb_authentication_status_title,
        detect
    ) { isEnabled ->
        coloredRow(
            R.string.adb_authentication_status_title,
            myApplication.getString(
                if (isEnabled) R.string.result_enabled else R.string.result_disabled
            ),
            isEnabled
        )
    }

    // The DSU row: the original else branch lands on the platform unknownName resource, not
    // result_not_supported; replicated as is.
    fun toDsuModel(detect: () -> DetectResult<Boolean>): MyModel = guardedEntry(
        R.string.dsu_status_title,
        detect
    ) { isEnabled ->
        coloredRow(
            R.string.dsu_status_title,
            myApplication.getString(
                if (isEnabled) R.string.result_supported else androidR.string.unknownName
            ),
            isEnabled
        )
    }

    // The dynamic partitions row: retrofit only appends text; enabled drives the two states
    // and the color.
    fun toDynamicPartitionsModel(
        detect: () -> DetectResult<DynamicPartitionsState>
    ): MyModel = guardedEntry(
        R.string.dynamic_partitions_status_title,
        detect
    ) { (enabled, retrofit) ->
        val detail = buildString {
            append(
                myApplication.getString(
                    if (enabled) R.string.result_supported else R.string.result_not_supported
                )
            )
            if (retrofit) {
                append(myApplication.getString(R.string.dynamic_partitions_enabled_retrofitted))
            }
        }
        coloredRow(R.string.dynamic_partitions_status_title, detail, enabled)
    }

    // The A/B seamless update row: the detail's three segments (supported or not, the virtual
    // A/B variant, the current slot) are all driven by the facts.
    fun toAbModel(detect: () -> DetectResult<AbState>): MyModel = guardedEntry(
        R.string.ab_seamless_update_status_title,
        detect
    ) { (enabled, virtualAb, virtualAbRetrofit, slotSuffix) ->
        val detail = buildString {
            append(
                myApplication.getString(
                    if (enabled) R.string.result_supported else R.string.result_not_supported
                )
            )
            if (enabled) {
                if (virtualAb) {
                    append(
                        myApplication.getString(
                            if (virtualAbRetrofit) R.string.virtual_ab_retrofit else R.string.virtual_ab
                        )
                    )
                }
                // A null slot suffix (absent property) renders the not-filled placeholder, the
                // same text the old property-reader path carried into the detail.
                val slotText = slotSuffix ?: myApplication.getString(R.string.build_not_filled)
                append(myApplication.getString(R.string.current_using_ab_slot_result, slotText))
            }
        }
        coloredRow(R.string.ab_seamless_update_status_title, detail, enabled)
    }

    // The lld mode row: online/offline wording and the color come from the source and the
    // payload presence; the rendered fetch-failure messages (ViewModel display material by
    // origin) are appended verbatim.
    fun toModeModel(detect: () -> DetectResult<LldStatusState>): MyModel = guardedEntry(
        R.string.lld_json_mode_title,
        detect
    ) { state ->
        val datetimeFormatted = state.version?.formatToLocalZonedDatetimeString()
            ?: myApplication.getString(androidR.string.unknownName)
        val modeDetailRes = when (state.source) {
            LldSource.ONLINE -> R.string.lld_json_online
            LldSource.OFFLINE -> R.string.lld_json_offline
        }
        val joinedErrors = state.errors.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n\n", prefix = "\n\n")
            .orEmpty()
        coloredRow(
            R.string.lld_json_mode_title,
            myApplication.getString(modeDetailRes, datetimeFormatted) + joinedErrors,
            if (state.version != null && state.source == LldSource.ONLINE) {
                StatusColor.NO_PROBLEM
            } else {
                StatusColor.CRITICAL
            }
        )
    }

    // The android info row: the preview suffix and the Go mark are display material added here;
    // the five reference lines carry nulls the mapper renders as the platform unknownName.
    fun toAndroidModel(detect: () -> DetectResult<AndroidInfoState>): MyModel = guardedEntry(
        R.string.android_info_title,
        detect
    ) { state ->
        val previewSuffix = when {
            !state.isPreview -> ""
            state.canaryLabel != null -> " ${state.canaryLabel}"
            else -> " ${myApplication.getString(R.string.android_info_preview)}"
        }
        val dessert = state.dessert ?: myApplication.getString(androidR.string.unknownName)
        var mine = "${state.version}$previewSuffix, $dessert"
        if (state.isGoEdition) {
            mine += " (Go)"
        }
        val mineLine = myApplication.getString(R.string.android_info, mine, state.apiFull)
        val referenceLines = state.referenceRows.map { row ->
            myApplication.getString(
                R.string.android_info,
                row.version ?: myApplication.getString(androidR.string.unknownName),
                row.apiFull ?: myApplication.getString(androidR.string.unknownName)
            )
        }
        val color = when (state.currency) {
            AndroidInfoState.AndroidCurrency.Latest -> StatusColor.NO_PROBLEM
            AndroidInfoState.AndroidCurrency.Supported -> StatusColor.WARNING
            AndroidInfoState.AndroidCurrency.Unknown, AndroidInfoState.AndroidCurrency.Outdated -> StatusColor.CRITICAL
        }
        coloredRow(
            R.string.android_info_title,
            myApplication.getString(
                R.string.android_info_detail,
                mineLine,
                referenceLines[0],
                referenceLines[1],
                referenceLines[2],
                referenceLines[3],
                referenceLines[4]
            ),
            color
        )
    }

    // The sdk extension row: a null current extension (below the declaring level) renders as
    // result_not_supported, a null stable extension as the platform unknownName.
    fun toSdkExtensionModel(detect: () -> DetectResult<SdkExtensionState>): MyModel = guardedEntry(
        R.string.android_sdk_extension_title,
        detect
    ) { state ->
        val current = state.currentExtension?.toString()
            ?: myApplication.getString(R.string.result_not_supported)
        val stable = state.stableExtension?.toString()
            ?: myApplication.getString(androidR.string.unknownName)
        coloredRow(
            R.string.android_sdk_extension_title,
            myApplication.getString(R.string.android_sdk_extension_detail, current, stable),
            state.meetsStable
        )
    }

    // The performance class row: NotSupported and NotDeclared share the result_not_supported
    // detail; the other levels carry the class number in the detail.
    fun toPerformanceClassModel(detect: () -> DetectResult<PerformanceClassInfo>): MyModel = guardedEntry(
        R.string.performance_class_title,
        detect
    ) { info ->
        val detail = when (info.level) {
            PerformanceClassInfo.Level.NotSupported, PerformanceClassInfo.Level.NotDeclared ->
                myApplication.getString(R.string.result_not_supported)
            PerformanceClassInfo.Level.Met, PerformanceClassInfo.Level.Lagging,
            PerformanceClassInfo.Level.Outdated ->
                myApplication.getString(
                    R.string.performance_class_detail_api,
                    info.performanceClass.toString()
                )
        }
        val color = when (info.level) {
            PerformanceClassInfo.Level.Met -> StatusColor.NO_PROBLEM
            PerformanceClassInfo.Level.Lagging -> StatusColor.WARNING
            PerformanceClassInfo.Level.NotSupported, PerformanceClassInfo.Level.NotDeclared,
            PerformanceClassInfo.Level.Outdated -> StatusColor.CRITICAL
        }
        coloredRow(R.string.performance_class_title, detail, color)
    }

    // The kernel row: the supported-versions list is joined here; a missing lld list renders
    // as the platform unknownName.
    fun toKernelModel(detect: () -> DetectResult<KernelState>): MyModel = guardedEntry(
        R.string.linux_title,
        detect
    ) { state ->
        val support = state.supportedVersions?.joinToString("｜")
            ?: myApplication.getString(androidR.string.unknownName)
        val mainline = state.mainlineVersion
            ?: myApplication.getString(androidR.string.unknownName)
        val color = when (state.match) {
            KernelState.KernelMatch.Met -> StatusColor.NO_PROBLEM
            KernelState.KernelMatch.Lagging -> StatusColor.WARNING
            KernelState.KernelMatch.None -> StatusColor.CRITICAL
        }
        coloredRow(
            R.string.linux_title,
            myApplication.getString(R.string.linux_version_detail, state.linuxVersionString, support, mainline),
            color
        )
    }

    // Two-state row color mapping: a true condition lands on NO_PROBLEM. Home-only, dots are a
    // Home-only concept (#69).
    private fun coloredRow(@StringRes titleRes: Int, detail: String, condition: Boolean): MyModel =
        MyModel(
            title = MyModelTitle.Res(titleRes),
            detail = detail,
            color = if (condition) StatusColor.NO_PROBLEM else StatusColor.CRITICAL
        )

    // The color-overload counterpart of the old toColoredMyModel: rows whose color the
    // repository judged from richer semantics (the mode, android, performance class and kernel
    // rows) hand the decided color straight in.
    private fun coloredRow(@StringRes titleRes: Int, detail: String, color: StatusColor): MyModel =
        MyModel(
            title = MyModelTitle.Res(titleRes),
            detail = detail,
            color = color
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

    // The chained thin guard's shared scaffold: a try around the whole fetch-and-render chain,
    // degrading unexpected exceptions into the entry's own failure row; [CancellationException]
    // always rethrows. Failed renders the failure row, Missing the unknown row (reason as the
    // detail, unreachable for most entries and kept for exhaustiveness). The failure DEBUG print
    // stays single-sited in [failureRow]. When the engine absorbs this scaffold (subtask-05),
    // the wrapping at the call side goes away.
    // onFound is the last parameter so the callers' trailing lambda binds to it (a trailing
    // lambda always binds to the last parameter).
    private fun <T> guardedEntry(
        @StringRes titleRes: Int,
        detect: () -> DetectResult<T>,
        onMissing: (String) -> MyModel = { unknownRow(titleRes, it) },
        onFound: (T) -> MyModel
    ): MyModel = try {
        when (val result = detect()) {
            is DetectResult.Found -> onFound(result.value)
            is DetectResult.Missing -> onMissing(result.reason)
            is DetectResult.Failed -> failureRow(titleRes, result.cause)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failureRow(titleRes, e)
    }
}
