package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.Keep
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import net.imknown.android.forefrontinfo.ui.common.CODENAME_NONE

const val EXTENSION_NONE = 0
const val SCHEME_VERSION = 1

@Keep
@Serializable
data class Lld(
    val scheme: Int,
    val version: String,
    val android: Androids,
    val linux: Linuxes,
    val toybox: Toyboxes,
    val webView: WebViews
) {
    // https://en.wikipedia.org/wiki/Android_version_history
    //
    // https://developer.android.com/about/dashboards?hl=en
    //
    // https://apilevels.com/
    // https://targetsdk.com/
    //
    // https://www.bidouille.org/misc/androidcharts
    // https://composables.com/android-distribution-chart
    // https://telemetrydeck.com/survey/android/Android/sdkVersions/
    // https://www.appbrain.com/stats/top-android-sdk-versions
    // https://gs.statcounter.com/android-version-market-share/
    @Keep
    @Serializable
    data class Androids(
        // https://source.android.com/security/bulletin#bulletins
        val securityPatchLevel: String,
        // https://source.android.com/security/bulletin#bulletins
        val googlePlaySystemUpdates: String,
        val build: Build,
        // https://www.android.com
        val stable: Android,
        // Beta
        // https://developer.android.com/preview/overview
        // https://developer.android.com/about/versions/16/release-notes
        // https://developer.android.com/about/versions/16/release-notes-qpr
        // https://developer.android.com/about/versions/16/qpr2/release-notes
        val stablePreview: Android,
        // https://source.android.com/security/bulletin/
        val support: Android,
        // Canary
        // https://ci.android.com
        // https://developer.android.com/about/canary
        val preview: Android,
        val internal: Android,
        val known: List<Android>
    ) {
        @Keep
        @Serializable
        data class Android(
            val api: String,
            val apiFull: String,
            val version: String,
            /** Dessert */
            val name: String,
            // https://android.googlesource.com/platform/build/release/+/refs/heads/main/flag_values/trunk_staging/RELEASE_PLATFORM_VERSION_KNOWN_CODENAMES.textproto
            @EncodeDefault val codename: String = CODENAME_NONE,
            @EncodeDefault val extension: Int = EXTENSION_NONE,
            /** Deprecated: "Preview" used to be: "$[version] $[phase]" */
            val phase: String? = null
        )

        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://developers.google.com/android/images
        // https://developers.google.com/android/ota
        // https://android.googlesource.com/platform/build/+refs
        // https://android.googlesource.com/platform/build/+/refs/tags/android-16.0.0_r2/core/build_id.mk
        // https://android.googlesource.com/platform/build/+/master/core/build_id.mk
        @Keep
        @Serializable
        data class Build(
            val version: String,
            val details: List<Detail>
        ) {
            @Keep
            @Serializable
            data class Detail(
                val id: String,
                val revision: String
            )
        }
    }

    // https://source.android.com/setup/build/building-kernels#downloading
    // https://source.android.com/devices/architecture/kernel/android-common
    // https://www.kernel.org
    // https://en.wikipedia.org/wiki/Linux_kernel_version_history
    @Keep
    @Serializable
    data class Linuxes(
        // https://android.googlesource.com/kernel/common
        val google: Versions,
        // https://android.googlesource.com/kernel/common/+/android-mainline/Makefile
        val mainline: BaseInfo
    ) {
        @Keep
        @Serializable
        data class Versions(
            val versions: List<String>
        )
    }

    // https://github.com/landley/toybox
    // https://android.googlesource.com/platform/external/toybox/+refs
    // https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/
    @Keep
    @Serializable
    data class Toyboxes(
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-16.0.0_r1/toys.h
        val stable: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-security-13.0.0_r1/toys.h#137
        val support: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/master/toys.h#144
        // https://android.googlesource.com/platform/external/toybox/+/upstream-master/toys.h#144
        val master: BaseInfo
    )

    // https://www.chromium.org/developers/calendar
    // https://chromiumdash.appspot.com/releases?platform=Android
    //
    // https://en.wikipedia.org/wiki/Google_Chrome_version_history
    @Keep
    @Serializable
    data class WebViews(
        val stable: BaseInfo,
        val beta: BaseInfo
    )
}
