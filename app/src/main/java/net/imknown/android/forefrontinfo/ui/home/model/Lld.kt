package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.Keep
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

const val CODENAME_NONE = ""
const val CODENAME_CANARY = "CANARY"
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
    // https://source.android.com/security/enhancements/enhancements9
    // https://source.android.com/setup/start/p-release-notes
    // https://developer.android.com/about/versions/10
    //
    // https://en.wikipedia.org/wiki/Android_version_history
    // https://developer.android.com/about/dashboards?hl=en
    // https://www.bidouille.org/misc/androidcharts
    //
    // https://mta.qq.com/mta/data/device/os
    // https://compass.umeng.com/#hardwareList
    // https://tongji.baidu.com/research/app
    // https://www.appbrain.com/stats/top-android-sdk-versions
    // https://gs.statcounter.com/android-version-market-share/
    @Keep
    @Serializable
    data class Androids(
        // https://source.android.com/security/bulletin#bulletins
        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://android.googlesource.com/platform/build/+/master/core/version_defaults.mk
        val securityPatchLevel: String,
        // https://source.android.com/security/bulletin#bulletins
        val googlePlaySystemUpdates: String,
        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://android.googlesource.com/platform/frameworks/base/+refs
        // https://developers.google.com/android/images
        // https://developers.google.com/android/ota
        val build: Build,
        // https://www.android.com
        val stable: Android,
        // Beta
        // https://developer.android.com/preview/overview
        // https://developer.android.com/about/versions/16/release-notes-qpr
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

        // https://android.googlesource.com/platform/build/+refs
        // https://android.googlesource.com/platform/build/+/refs/tags/android-12.0.0_r29/core/build_id.mk
        // https://android.googlesource.com/platform/build/+/master/core/build_id.mk
        //
        // https://developers.google.com/android/images
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
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-12.0.0_r29/toys.h
        val stable: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-security-10.0.0_r63/main.c
        val support: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/master/toys.h#135
        // https://android.googlesource.com/platform/external/toybox/+/upstream-master/toys.h#135
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
