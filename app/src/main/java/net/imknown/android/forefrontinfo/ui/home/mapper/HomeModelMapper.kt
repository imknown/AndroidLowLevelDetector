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
import net.imknown.android.forefrontinfo.ui.base.list.MyModelType
import net.imknown.android.forefrontinfo.ui.home.model.AbState
import net.imknown.android.forefrontinfo.ui.home.model.AndroidInfoState
import net.imknown.android.forefrontinfo.ui.home.model.ApexState
import net.imknown.android.forefrontinfo.ui.home.model.BuildIdState
import net.imknown.android.forefrontinfo.ui.home.model.DynamicPartitionsState
import net.imknown.android.forefrontinfo.ui.home.model.EncryptionState
import net.imknown.android.forefrontinfo.ui.home.model.GsiState
import net.imknown.android.forefrontinfo.ui.home.model.KernelState
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import net.imknown.android.forefrontinfo.ui.home.model.LldStatusState
import net.imknown.android.forefrontinfo.ui.home.model.MainlineState
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import net.imknown.android.forefrontinfo.ui.home.model.PerformanceClassInfo
import net.imknown.android.forefrontinfo.ui.home.model.SELinuxState
import net.imknown.android.forefrontinfo.ui.home.model.SarState
import net.imknown.android.forefrontinfo.ui.home.model.SecurityPatchState
import net.imknown.android.forefrontinfo.ui.home.model.SdkExtensionState
import net.imknown.android.forefrontinfo.ui.home.model.ToyboxState
import net.imknown.android.forefrontinfo.ui.home.model.TrebleState
import net.imknown.android.forefrontinfo.ui.home.model.VndkState
import net.imknown.android.forefrontinfo.ui.home.model.WebViewState
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

    // The build id row: the partition ids render verbatim (a failed or unmet property read
    // carries the placeholder it produced, until the #42 rework), and the lld details join
    // with one entry per line.
    fun toBuildIdModel(detect: () -> DetectResult<BuildIdState>): MyModel = guardedEntry(
        R.string.android_build_id_title,
        detect
    ) { state ->
        val builds = state.lldDetails?.joinToString("\n") { (id, revision) ->
            myApplication.getString(R.string.android_build_id, id, revision)
        } ?: myApplication.getString(androidR.string.unknownName)
        coloredRow(
            R.string.android_build_id_title,
            myApplication.getString(
                R.string.android_build_id_detail,
                state.buildId,
                state.systemBuildId,
                state.vendorBuildId,
                state.odmBuildId,
                state.lldBuildVersion ?: myApplication.getString(androidR.string.unknownName),
                builds
            ),
            when (state.match) {
                BuildIdState.Match.Met -> StatusColor.NO_PROBLEM
                BuildIdState.Match.Lagging -> StatusColor.WARNING
                BuildIdState.Match.Unknown, BuildIdState.Match.Outdated -> StatusColor.CRITICAL
            }
        )
    }

    // The two security patch rows ride in the system-then-vendor order, each keeping its own
    // guard and title (the old per-row guardedMyModel).
    fun toSecurityPatchModels(detect: () -> List<DetectResult<SecurityPatchState>>): List<MyModel> =
        detect().mapIndexed { index, result ->
            val titleRes = if (index == 0) {
                R.string.security_patch_level_title
            } else {
                R.string.vendor_security_patch_level_title
            }
            guardedEntry(titleRes, { result }) { state ->
                coloredRow(
                    titleRes,
                    myApplication.getString(
                        R.string.security_patch_level_detail,
                        state.patch,
                        state.lldPatchLevel ?: myApplication.getString(androidR.string.unknownName)
                    ),
                    when (state.match) {
                        SecurityPatchState.Match.Met -> StatusColor.NO_PROBLEM
                        SecurityPatchState.Match.Lagging -> StatusColor.WARNING
                        SecurityPatchState.Match.Unknown, SecurityPatchState.Match.None -> StatusColor.CRITICAL
                    }
                )
            }
        }

    // The VNDK row: an absent version renders as result_not_supported, a present one as the
    // supported text plus the level line (the Lite variant marked in the level).
    fun toVndkModel(detect: () -> DetectResult<VndkState>): MyModel = guardedEntry(
        R.string.vndk_built_in_title,
        detect
    ) { state ->
        val detail = if (state.vndkVersion == null) {
            myApplication.getString(R.string.result_not_supported)
        } else {
            myApplication.getString(R.string.result_supported) + myApplication.getString(
                R.string.built_in_vndk_version_result,
                if (state.hasVndkLite) "${state.vndkVersion}, Lite" else state.vndkVersion
            )
        }
        coloredRow(
            R.string.vndk_built_in_title,
            detail,
            when (state.match) {
                VndkState.Match.Met -> StatusColor.NO_PROBLEM
                VndkState.Match.Lagging -> StatusColor.WARNING
                VndkState.Match.Unknown, VndkState.Match.None -> StatusColor.CRITICAL
            }
        )
    }

    // The mainline row: a provider or version that was never fetched renders as the platform
    // unknownName / result_not_supported; an installed package without a version renders the
    // empty string verbatim.
    fun toMainlineModel(detect: () -> DetectResult<MainlineState>): MyModel = guardedEntry(
        R.string.mainline_title,
        detect
    ) { state ->
        coloredRow(
            R.string.mainline_title,
            myApplication.getString(
                R.string.mainline_detail,
                state.versionName ?: myApplication.getString(R.string.result_not_supported),
                state.moduleProvider ?: myApplication.getString(androidR.string.unknownName),
                state.latestGooglePlaySystemUpdates ?: myApplication.getString(androidR.string.unknownName)
            ),
            when (state.match) {
                MainlineState.Match.Met -> StatusColor.NO_PROBLEM
                MainlineState.Match.Unknown, MainlineState.Match.None -> StatusColor.CRITICAL
            }
        )
    }

    // The SAR row: the fact's type picks the appended wording; a present-but-unmatched SAR
    // (Unknown) renders the platform unknownName, the old else branch. Legacy and recovery are
    // yellow, 2SI and slash green; no SAR or unmatched is red.
    fun toSarModel(detect: () -> DetectResult<SarState>): MyModel = guardedEntry(
        R.string.sar_status_title,
        detect
    ) { state ->
        val detail = buildString {
            append(
                myApplication.getString(
                    if (state.supported) R.string.result_supported else R.string.result_not_supported
                )
            )
            when (state.type) {
                SarState.SarType.Legacy -> append(" (${myApplication.getString(R.string.sar_type_legacy)})")
                SarState.SarType.TwoSi -> append(" (${myApplication.getString(R.string.sar_type_2si)})")
                SarState.SarType.Recovery -> append(" (${myApplication.getString(R.string.sar_type_recovery)})")
                SarState.SarType.Slash -> append(" (${myApplication.getString(R.string.sar_type_slash)})")
                SarState.SarType.Unknown -> append(" (${myApplication.getString(androidR.string.unknownName)})")
                null -> Unit
            }
        }
        coloredRow(
            R.string.sar_status_title,
            detail,
            when (state.type) {
                SarState.SarType.Legacy, SarState.SarType.Recovery -> StatusColor.WARNING
                SarState.SarType.TwoSi, SarState.SarType.Slash -> StatusColor.NO_PROBLEM
                SarState.SarType.Unknown, null -> StatusColor.CRITICAL
            }
        )
    }

    // The treble and GSI rows ride on one detection, fetched once here outside the per-row
    // guards: safe because both repository halves catch their own exceptions into Failed and only
    // rethrow CancellationException. The per-row guardedEntry then wraps each row's rendering
    // (the old per-row guardedMyModel): treble reads its vintf layout (append-only wording for
    // the two degraded layouts), GSI renders its four outcomes.
    fun toTrebleAndGsiModels(
        detect: () -> Pair<DetectResult<TrebleState>, DetectResult<GsiState>>
    ): List<MyModel> {
        val (trebleResult, gsiResult) = detect()
        return listOf(
            guardedEntry(R.string.treble_status_title, { trebleResult }) { state ->
                val detail = buildString {
                    append(
                        myApplication.getString(
                            if (state.enabled) R.string.result_supported else R.string.result_not_supported
                        )
                    )
                    when (state.vintf) {
                        TrebleState.Vintf.LegacyNoFragments ->
                            append(myApplication.getString(R.string.treble_legacy_no_fragments))
                        TrebleState.Vintf.Other ->
                            append(myApplication.getString(R.string.treble_other))
                        TrebleState.Vintf.Present, null -> Unit
                    }
                }
                coloredRow(
                    R.string.treble_status_title,
                    detail,
                    when (state.vintf) {
                        TrebleState.Vintf.Present -> StatusColor.NO_PROBLEM
                        TrebleState.Vintf.LegacyNoFragments, TrebleState.Vintf.Other -> StatusColor.WARNING
                        null -> StatusColor.CRITICAL
                    }
                )
            },
            guardedEntry(R.string.gsi_status_title, { gsiResult }) { state ->
                coloredRow(
                    R.string.gsi_status_title,
                    when (state.compatibility) {
                        GsiState.Compatibility.Compatible -> myApplication.getString(R.string.result_compliant)
                        GsiState.Compatibility.NotCompatible -> myApplication.getString(R.string.result_not_compliant)
                        GsiState.Compatibility.Unidentified -> myApplication.getString(R.string.result_unidentified)
                        GsiState.Compatibility.NotSupported -> myApplication.getString(R.string.result_not_supported)
                    },
                    when (state.compatibility) {
                        GsiState.Compatibility.Compatible -> StatusColor.NO_PROBLEM
                        GsiState.Compatibility.NotCompatible, GsiState.Compatibility.Unidentified -> StatusColor.WARNING
                        GsiState.Compatibility.NotSupported -> StatusColor.CRITICAL
                    }
                )
            }
        )
    }

    // The APEX row: updatable is green outright; not updatable with a mounted flattened APEX is
    // the legacy shape, yellow plus the appended wording; neither is red.
    fun toApexModel(detect: () -> DetectResult<ApexState>): MyModel = guardedEntry(
        R.string.apex_status_title,
        detect
    ) { state ->
        val supported = state.updatable || state.flattenedMounted
        val isLegacyFlattenedApex = !state.updatable && state.flattenedMounted
        var detail = myApplication.getString(
            if (supported) R.string.result_supported else R.string.result_not_supported
        )
        if (isLegacyFlattenedApex) {
            detail += myApplication.getString(R.string.apex_legacy_flattened)
        }
        coloredRow(
            R.string.apex_status_title,
            detail,
            when {
                state.updatable -> StatusColor.NO_PROBLEM
                isLegacyFlattenedApex -> StatusColor.WARNING
                else -> StatusColor.CRITICAL
            }
        )
    }

    // The toybox row: the read's three shapes each pick the first detail argument (the raw
    // stdout line / the platform unknown / the not-supported text); the lld reference versions
    // render the platform unknown when absent; the match decides the color.
    fun toToyboxModel(detect: () -> DetectResult<ToyboxState>): MyModel = guardedEntry(
        R.string.toybox_built_in_title,
        detect
    ) { state ->
        val deviceVersionText = when (val read = state.read) {
            is ToyboxState.ToyboxRead.Version -> read.value
            ToyboxState.ToyboxRead.NoOutput -> myApplication.getString(androidR.string.unknownName)
            ToyboxState.ToyboxRead.CommandFailed -> myApplication.getString(R.string.result_not_supported)
        }
        coloredRow(
            R.string.toybox_built_in_title,
            myApplication.getString(
                R.string.toybox_built_in_detail,
                deviceVersionText,
                state.stableVersion ?: myApplication.getString(androidR.string.unknownName),
                state.supportVersion ?: myApplication.getString(androidR.string.unknownName),
                state.masterVersion ?: myApplication.getString(androidR.string.unknownName)
            ),
            when (state.match) {
                ToyboxState.Match.Met -> StatusColor.NO_PROBLEM
                ToyboxState.Match.Lagging -> StatusColor.WARNING
                ToyboxState.Match.None -> StatusColor.CRITICAL
            }
        )
    }

    // The encryption row: the platform status values pick wording and color; the Unknown case
    // (transitioning or unrecognized statuses, missing DevicePolicyManager included) shares the
    // not-supported wording with Unsupported, the old else branch.
    fun toEncryptionModel(detect: () -> DetectResult<EncryptionState>): MyModel = guardedEntry(
        R.string.encryption_status_title,
        detect
    ) { state ->
        val detailRes: Int
        val color: StatusColor
        when (state.status) {
            EncryptionState.Status.Active, EncryptionState.Status.ActivePerUser -> {
                detailRes = R.string.result_encrypted
                color = StatusColor.NO_PROBLEM
            }
            EncryptionState.Status.ActiveDefaultKey -> {
                detailRes = R.string.result_encrypted_no_key_set
                color = StatusColor.WARNING
            }
            EncryptionState.Status.Inactive -> {
                detailRes = R.string.result_not_encrypted
                color = StatusColor.CRITICAL
            }
            EncryptionState.Status.Unsupported, EncryptionState.Status.Unknown -> {
                detailRes = R.string.result_not_supported
                color = StatusColor.CRITICAL
            }
        }
        coloredRow(R.string.encryption_status_title, myApplication.getString(detailRes), color)
    }

    // The SELinux row: the mode values pick wording and color; Unknown renders the platform
    // unknown name, the old else branch in both the success and the failure path.
    fun toSELinuxModel(detect: () -> DetectResult<SELinuxState>): MyModel = guardedEntry(
        R.string.selinux_status,
        detect
    ) { state ->
        val detailRes: Int
        val color: StatusColor
        when (state.mode) {
            SELinuxState.Mode.Enforcing -> {
                detailRes = R.string.selinux_status_enforcing_mode
                color = StatusColor.NO_PROBLEM
            }
            SELinuxState.Mode.Permissive -> {
                detailRes = R.string.selinux_status_permissive_mode
                color = StatusColor.WARNING
            }
            SELinuxState.Mode.Disabled -> {
                detailRes = R.string.result_disabled
                color = StatusColor.CRITICAL
            }
            SELinuxState.Mode.Unknown -> {
                detailRes = androidR.string.unknownName
                color = StatusColor.CRITICAL
            }
        }
        coloredRow(R.string.selinux_status, myApplication.getString(detailRes), color)
    }

    // The WebView row: the detail is one blob assembled here, segment by segment the same way the
    // old path built it. The provider list joins with the ending text as the separator (the old
    // code appended it between entries); an empty candidate list renders the not-filled
    // placeholder. The implementation block interpolates its nullable fields directly: a null
    // renders the literal "null" in the template, exactly like the old code. The match decides
    // the color.
    fun toWebViewModel(detect: () -> DetectResult<WebViewState>): MyModel = guardedEntry(
        R.string.webview_title,
        detect
    ) { state ->
        val type = if (state.isQOrLater) {
            val standalone = myApplication.getString(R.string.webview_standalone)
            myApplication.getString(R.string.webview_or, "Trichrome", standalone)
        } else {
            "Monochrome"
        }

        val builtInResult = if (state.builtInProviders.isEmpty()) {
            myApplication.getString(R.string.build_not_filled)
        } else {
            state.builtInProviders.joinToString(
                myApplication.getString(R.string.webview_ending)
            ) { (packageName, description, availableByDefault, isFallback, hasSignatures, installState) ->
                fun subFormat(text: String) =
                    myApplication.getString(R.string.webview_sub_format, text)

                fun subFormat(@StringRes stringRes: Int) =
                    subFormat(myApplication.getString(stringRes))

                val isInstalled = when (installState) {
                    is WebViewState.InstallState.Installed ->
                        installState.versionName
                            ?: myApplication.getString(androidR.string.unknownName)
                    WebViewState.InstallState.NotInstalled ->
                        myApplication.getString(R.string.webview_built_in_not_installed)
                }

                var tempResult = (packageName
                    ?: myApplication.getString(R.string.build_not_filled)) +
                        subFormat(description ?: myApplication.getString(R.string.build_not_filled)) +
                        subFormat(isInstalled)
                if (!availableByDefault) {
                    tempResult += subFormat(R.string.webview_must_be_chosen_by_user)
                }
                if (isFallback) {
                    tempResult += subFormat(R.string.webview_fallback)
                }
                if (hasSignatures) {
                    tempResult += subFormat(R.string.webview_signed)
                }
                tempResult
            }
        }

        val implementResult = """
                |${state.implement?.packageName}
                |  ${state.implement?.label}
                |  ${state.implement?.versionName}
                """.trimMargin()

        val latestResult = myApplication.getString(
            R.string.webview_detail,
            state.stableVersion ?: myApplication.getString(androidR.string.unknownName),
            state.betaVersion ?: myApplication.getString(androidR.string.unknownName)
        )

        coloredRow(
            R.string.webview_title,
            """
            |${myApplication.getString(R.string.webview_built_in_version)}: $type
            |
            |$builtInResult
            |
            |${myApplication.getString(R.string.webview_implement_version)}:
            |$implementResult
            |
            |$latestResult
            """.trimMargin(),
            when (state.match) {
                WebViewState.Match.Met -> StatusColor.NO_PROBLEM
                WebViewState.Match.Lagging -> StatusColor.WARNING
                WebViewState.Match.None -> StatusColor.CRITICAL
            }
        )
    }

    // The outdated target SDK row: the only typed row (subtask-01 addresses it by
    // [MyModelType.OutdatedTargetSdkApk]), so the MyModel is built here directly instead of via
    // coloredRow. The first API level line joins the list without a separator (the old code
    // appended joinToString's output straight onto it), and the order decides both the list
    // format and the sorting the repository already applied.
    fun toOutdatedTargetSdkVersionApkModel(
        detect: () -> DetectResult<OutdatedTargetSdkVersionApkState>
    ): MyModel = guardedEntry(
        R.string.outdated_target_version_sdk_version_apk_title,
        detect
    ) { (firstApiLevel, outdatedApks, order, match) ->
        var result = myApplication.getString(
            R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
            firstApiLevel
        )
        if (outdatedApks.isEmpty()) {
            result += myApplication.getString(
                R.string.outdated_target_version_sdk_version_apk_result_none
            )
        } else {
            val format = when (order) {
                OutdatedTargetSdkVersionApkState.Order.PackageNameFirst ->
                    R.string.outdated_target_version_sdk_version_apk_result_format_package_first
                OutdatedTargetSdkVersionApkState.Order.TargetSdkVersionFirst ->
                    R.string.outdated_target_version_sdk_version_apk_result_format_target_sdk_version_first
            }
            result += outdatedApks.joinToString("\n") { (packageName, targetSdkVersion) ->
                "- " + myApplication.getString(format, packageName, targetSdkVersion)
            }
        }

        MyModel(
            title = MyModelTitle.Res(R.string.outdated_target_version_sdk_version_apk_title),
            detail = result,
            color = when (match) {
                OutdatedTargetSdkVersionApkState.Match.Met -> StatusColor.NO_PROBLEM
                OutdatedTargetSdkVersionApkState.Match.Lagging -> StatusColor.WARNING
                OutdatedTargetSdkVersionApkState.Match.Unknown,
                OutdatedTargetSdkVersionApkState.Match.None -> StatusColor.CRITICAL
            },
            type = MyModelType.OutdatedTargetSdkApk
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
