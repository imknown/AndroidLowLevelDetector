package net.imknown.android.forefrontinfo.ui.home.model

import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.detect.Detector
import net.imknown.android.forefrontinfo.ui.base.detect.detector
import net.imknown.android.forefrontinfo.ui.base.detect.rawDetector
import net.imknown.android.forefrontinfo.ui.base.list.MyModel

/**
 * The Home detection entries' stable ids. The declaration order is the display order the Home
 * list assembles (the view model's detector list follows it) and matches the row order of the
 * detection-entry catalog; the [id] strings are the code-facing join key until the catalog
 * carries them too.
 */
enum class HomeEntryId(val id: String) {
    HOME_DETECT_MODE("home-detect-mode"),
    HOME_ANDROID_VERSION("home-android-version"),
    HOME_SDK_EXTENSION_VERSION("home-sdk-extension-version"),
    HOME_BUILD_ID("home-build-id"),
    HOME_SECURITY_PATCH("home-security-patch"),
    HOME_PERFORMANCE_CLASS("home-performance-class"),
    HOME_KERNEL("home-kernel"),
    HOME_AB_UPDATE("home-ab-update"),
    HOME_SAR("home-sar"),
    HOME_DYNAMIC_PARTITIONS("home-dynamic-partitions"),
    HOME_TREBLE_GSI("home-treble-gsi"),
    HOME_DSU("home-dsu"),
    HOME_MAINLINE("home-mainline"),
    HOME_VNDK("home-vndk"),
    HOME_APEX("home-apex"),
    HOME_DEVELOPER_OPTIONS("home-developer-options"),
    HOME_ADB("home-adb"),
    HOME_ADB_AUTHENTICATION("home-adb-authentication"),
    HOME_ENCRYPTION("home-encryption"),
    HOME_SELINUX("home-selinux"),
    HOME_TOYBOX("home-toybox"),
    HOME_WEBVIEW("home-webview"),
    HOME_OUTDATED_TARGET_SDK_APK("home-outdated-target-sdk-apk")
}

/**
 * [Detector] factories keyed by [HomeEntryId]: the overloads convert the enum to its id string
 * so the generic engine stays page-agnostic.
 */
fun <T> detector(
    id: HomeEntryId,
    exec: () -> DetectResult<T>,
    translate: (DetectResult<T>) -> List<MyModel>
): Detector = detector(id.id, exec = exec, translate = translate)

fun rawDetector(
    id: HomeEntryId,
    run: () -> List<MyModel>,
    failureRows: (Exception) -> List<MyModel>
): Detector = rawDetector(id.id, run = run, failureRows = failureRows)
