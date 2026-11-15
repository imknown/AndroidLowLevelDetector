package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import io.github.g00fy2.versioncompare.Version
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.common.isPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isStableAndroid
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidVersionDataSource
import net.imknown.android.forefrontinfo.ui.home.model.BuildIdState
import net.imknown.android.forefrontinfo.ui.home.model.KernelState
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.MainlineState
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import net.imknown.android.forefrontinfo.ui.home.model.SecurityPatchState
import net.imknown.android.forefrontinfo.ui.home.model.ToyboxState
import net.imknown.android.forefrontinfo.ui.home.model.VndkState
import net.imknown.android.forefrontinfo.ui.home.model.WebViewState

/**
 * The Home comparison entries' lld-facing judgments, named and individually testable (#43):
 * the Android currency bars, the BuildId date comparison with its preview offset, and the
 * per-entry matches against the lld's reference versions and dates. The bodies moved verbatim
 * out of [HomeRepository]'s inline when-chains and private members, with exactly one
 * exception: [getSecurityPatchYearMonth] carries the month-precision fix (the batch's only
 * behavior change, locked by DetectSecurityPatchesTest). Every other body is a verbatim
 * relocation, and the characterization tests pin it.
 */

internal fun isLatestStableAndroid(lld: Lld) =
    isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

internal fun isLatestPreviewAndroid(lld: Lld) =
    isPreviewAndroid() && AndroidVersionDataSource.currentAndroidInfo.apiFull >= lld.android.preview.apiFull

// The device build date against the lld's first detail date: a CTS-style three-segment
// build id carries the date in its second segment, and a device on the preview channel
// compares with the preview offset.
internal fun isBuildIdDateHigherThanConfig(
    lld: Lld,
    buildIdResult: String,
    systemBuildIdResult: String,
    lldDetails: List<BuildIdState.BuildDetail>?
): Boolean {
    fun isBuildIdAndroid8CtsFormat(buildId: String) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR).size >= 3

    val myBuildIdForCompare = if (
        isBuildIdAndroid8CtsFormat(buildIdResult) || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    ) {
        buildIdResult
    } else {
        systemBuildIdResult
    }

    if (!isBuildIdAndroid8CtsFormat(myBuildIdForCompare)) {
        return false
    }

    fun getDate(buildId: String) = buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR)[1] // "250705"

    val myBuildIdDate = getDate(myBuildIdForCompare)
    val myBuildIdDateIntOrNull = myBuildIdDate.toIntOrNull()
        ?: return false

    val lldFirstDetail = lldDetails?.getOrNull(0)
        ?: return false
    val lldFirstBuildId = lldFirstDetail.id
    val lldFirstBuildIdDate = getDate(lldFirstBuildId)
    val lldFirstBuildIdDateIntOrNull = lldFirstBuildIdDate.toIntOrNull()
        ?: return false

    val offset = if (isLatestStableAndroid(lld)) {
        0
    } else {
        /**
         * [isLatestPreviewAndroid] The preview grace window, introduced with the Baklava
         * build-id rules (5c72866f) and owner-confirmed: the newest RELEASED preview build
         * dates a little BEHIND the same-period stable release, so a preview-channel device
         * sits below the lld's reference date and the offset lifts it back. Caution (#27):
         * the subtraction runs on yymmdd integers, so 104 is NOT a day count (the two dates
         * are 35 days apart); it is a calibration margin for that season's preview cycle,
         * silently stale once the season turns; re-derive the pair each season.
         * DetectBuildIdTest's preview case locks the arithmetic.
         */
        250205 - 250101
    }

    return (myBuildIdDateIntOrNull + offset) >= lldFirstBuildIdDateIntOrNull
}

// The device patch level against the lld's: a full string compare first, then a year-month
// fallback so a device behind only in days (or reporting year-month precision) lags instead
// of failing outright.
internal fun securityPatchMatch(
    mySecurityPatch: String,
    lldSecurityPatch: String?
): SecurityPatchState.Match = when {
    lldSecurityPatch == null -> SecurityPatchState.Match.Unknown
    !isPropertyValueNotEmpty(mySecurityPatch) -> SecurityPatchState.Match.None
    mySecurityPatch >= lldSecurityPatch -> SecurityPatchState.Match.Met
    getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> SecurityPatchState.Match.Lagging
    else -> SecurityPatchState.Match.None
}

internal fun getSecurityPatchYearMonth(securityPatch: String) =
    securityPatch.split('-').take(2).joinToString("-")

// A property value is only a value when it is non-empty and not one of the localized
// placeholder strings the readers produce (the #42 remnant; the rework replaces the
// placeholders themselves).
internal fun isPropertyValueNotEmpty(result: String) =
    result.isNotEmpty()
        && result != MyApplication.getMyString(R.string.result_not_supported)
        && result != MyApplication.getMyString(R.string.build_not_filled)

// The device kernel against the lld's supported list: the first entry whose major.minor
// matches decides, and a device below that entry's full version lags.
internal fun kernelMatch(
    linuxVersion: Version,
    versionsSupported: List<String>?
): KernelState.KernelMatch {
    var match = KernelState.KernelMatch.None
    versionsSupported?.forEach {
        if (linuxVersion.major == Version(it).major
            && linuxVersion.minor == Version(it).minor
        ) {
            match = if (linuxVersion.isAtLeast(it)) {
                KernelState.KernelMatch.Met
            } else {
                KernelState.KernelMatch.Lagging
            }

            return@forEach
        }
    }

    return match
}

// The comparison was designed for date-form versionNames (the "-01" suffix lifts a
// month-precision date to the lld's day precision), and real-world input mostly complies:
// com.google.android.modulemetadata reports a date, Google API system images a date with a
// suffix ("2023-07-01 S+", the date prefix dominates). The intruder is
// com.android.modulemetadata on real devices, which reports the system version ("14"):
// against a date that is a dimension mismatch, "1"-prefixed versions (Android 10-19) lose
// the compare for every release, 3-prefixed hypotheticals win it. Locked as-is; #70 owns
// the redesign and the tests pin the misjudgments.
internal fun mainlineMatch(
    versionName: String?,
    latestGooglePlaySystemUpdates: String?
): MainlineState.Match = when {
    latestGooglePlaySystemUpdates == null -> MainlineState.Match.Unknown
    versionName != null && versionName >= latestGooglePlaySystemUpdates -> MainlineState.Match.Met
    versionName != null && "${versionName}-01" >= latestGooglePlaySystemUpdates -> MainlineState.Match.Met
    else -> MainlineState.Match.None
}

// The comparison direction follows #67, which stays deferred: VndkState.Match.Met is decided
// here exactly as before, and the semantics are deliberately not pinned by new tests.
internal fun vndkMatch(
    vndkVersion: String?,
    hasVndkLite: Boolean,
    lld: Lld?
): VndkState.Match = when {
    vndkVersion == null -> VndkState.Match.None
    lld == null -> VndkState.Match.Unknown
    (isLatestPreviewAndroid(lld) || vndkVersion >= lld.android.stable.api) && !hasVndkLite -> VndkState.Match.Met
    else -> VndkState.Match.Lagging
}

// The version ladder only runs on a real read. The old path fed the unknown text into
// versioncompare when the command succeeded without output (practically always landing on
// the critical else); NoOutput decides None directly (review v1 confirmed the outputs are
// identical on every reachable input).
internal fun toyboxMatch(
    read: ToyboxState.ToyboxRead,
    stableVersion: String?,
    supportVersion: String?
): ToyboxState.Match = when (read) {
    is ToyboxState.ToyboxRead.Version -> {
        val toyboxRealVersion = Version(read.value.replace("toybox ", ""))
        when {
            stableVersion != null && toyboxRealVersion.isAtLeast(stableVersion) -> ToyboxState.Match.Met
            supportVersion != null && toyboxRealVersion.isAtLeast(supportVersion) -> ToyboxState.Match.Lagging
            else -> ToyboxState.Match.None
        }
    }
    ToyboxState.ToyboxRead.NoOutput, ToyboxState.ToyboxRead.CommandFailed -> ToyboxState.Match.None
}

// The built-in providers' highest version decides Met; the currently implementing package
// (Version(null) compares as 0.0) decides Lagging.
internal fun webViewMatch(
    maxBuiltInVersionName: String,
    implementVersionName: String?,
    lldWebViewStable: String?
): WebViewState.Match = when {
    lldWebViewStable != null && Version(maxBuiltInVersionName).isAtLeast(lldWebViewStable) ->
        WebViewState.Match.Met
    lldWebViewStable != null && Version(implementVersionName).isAtLeast(lldWebViewStable) ->
        WebViewState.Match.Lagging
    else -> WebViewState.Match.None
}

// An empty outdated list means the channel judgment; a non-empty list renders its rows and
// the match stays None (the outdatedApk type carries the information).
internal fun outdatedTargetSdkApkMatch(
    outdatedApks: List<OutdatedTargetSdkVersionApkState.OutdatedApk>,
    lld: Lld?
): OutdatedTargetSdkVersionApkState.Match = when {
    outdatedApks.isNotEmpty() -> OutdatedTargetSdkVersionApkState.Match.None
    lld == null -> OutdatedTargetSdkVersionApkState.Match.Unknown
    isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) ->
        OutdatedTargetSdkVersionApkState.Match.Met
    else -> OutdatedTargetSdkVersionApkState.Match.Lagging
}

// The device's SDK extension version against the lld's stable reference: null on the lld
// side means no bar to meet.
internal fun meetsStableExtension(myExtension: Int, lldStableExtension: Int?) =
    lldStableExtension != null && myExtension >= lldStableExtension

// The device's Android at or above the lld's upstream support bar: the currency chain's
// third rung, behind Latest and before Outdated.
internal fun isSupportedByUpstreamAndroid(lld: Lld) =
    isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
