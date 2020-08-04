package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.Keep

@Keep
data class Lld(
    val android: Androids,
    val linux: Linuxes,
    val toybox: Toyboxes,
    val webView: WebViews
) : BaseInfo() {
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
    data class Androids(
        // https://source.android.com/security/bulletin/
        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://android.googlesource.com/platform/build/+/master/core/version_defaults.mk
        val securityPatchLevel: String,
        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://android.googlesource.com/platform/frameworks/base/+refs
        val build: Build,
        // https://www.android.com
        val stable: Android,
        // https://source.android.com/security/bulletin/
        val support: Android,
        // https://ci.android.com
        // https://developer.android.com/preview/overview
        val preview: Android
    ) {
        @Keep
        data class Android(
            val name: String,
            val api: String,
            val phase: String?
        ) : BaseInfo()

        // https://android.googlesource.com/platform/build/+refs
        // https://android.googlesource.com/platform/build/+/refs/tags/android-10.0.0_r41/core/build_id.mk
        // https://android.googlesource.com/platform/build/+/master/core/build_id.mk
        //
        // https://developers.google.com/android/images
        @Keep
        data class Build(
            val details: List<Detail>
        ) : BaseInfo() {
            @Keep
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
    data class Linuxes(
        // https://android.googlesource.com/kernel/common/+/android-5.4/Makefile
        // https://android.googlesource.com/kernel/common/+/android-4.19/Makefile
        // https://android.googlesource.com/kernel/common/+/android-4.14/Makefile
        // https://android.googlesource.com/kernel/common/+/android-4.9-q/Makefile
        // https://android.googlesource.com/kernel/common/+/android-4.4-p/Makefile
        // https://android.googlesource.com/kernel/common/+/android-3.18/Makefile
        val google: Versions,
        // https://android.googlesource.com/kernel/common/+/android-mainline/Makefile
        val mainline: BaseInfo
    ) {
        @Keep
        data class Versions(
            val versions: List<String>
        )
    }

    // https://github.com/landley/toybox
    // https://android.googlesource.com/platform/external/toybox/+refs
    // https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/
    @Keep
    data class Toyboxes(
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-10.0.0_r41/main.c
        val stable: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/refs/tags/android-8.0.0_r49/main.c
        val support: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/android10-mainline-resolv-release/main.c
        val mainline: BaseInfo,
        // https://android.googlesource.com/platform/external/toybox/+/master/toys.h#135
        // https://android.googlesource.com/platform/external/toybox/+/upstream-master/toys.h#135
        val master: BaseInfo
    )

    // https://www.chromium.org/developers/calendar
    // https://chromiumdash.appspot.com/releases?platform=Android
    //
    // https://en.wikipedia.org/wiki/Google_Chrome_version_history
    @Keep
    data class WebViews(
        val stable: BaseInfo,
        val beta: BaseInfo
    )
}