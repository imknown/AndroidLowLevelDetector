package net.imknown.android.forefrontinfo.ui.home.datasource

import android.os.Build
import android.util.Log
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.common.CODENAME_RELEASE
import net.imknown.android.forefrontinfo.ui.common.toPascalCase
import kotlin.reflect.KClass

/** The device's Android version facts; fields align with [net.imknown.android.forefrontinfo.ui.home.model.Lld.Androids.Android]. */
data class AndroidInfo(
    val api: Int, // 36
    val apiFull: String, // "35", "36.0", "36.1"
    val version: String, // "15", "16", "16.1 (QPR2)"
    val dessert: String? // "Baklava 1"
)

/** One [Build.VERSION_CODES] / [Build.VERSION_CODES_FULL] entry: its numeric value plus dessert name. */
data class VersionCode(val api: Int, val dessert: String)

/**
 * The single source for the device's Android version facts (issues-cn #09): no mutable global,
 * no Application-startup hook. The lld known correction is display-only and lives in
 * `HomeRepository.detectAndroid()`; API-level gating reads `Build.VERSION.SDK_INT` directly.
 */
object AndroidVersionDataSource {

    /** Deriving walks `Build.VERSION_CODES(_FULL)` via reflection, so it runs once per process. */
    val currentAndroidInfo: AndroidInfo by lazy {
        val kClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            Build.VERSION_CODES_FULL::class
        } else {
            Build.VERSION_CODES::class
        }
        deriveAndroidInfo(
            sdkInt = Build.VERSION.SDK_INT,
            runtimeSdkIntFull = Build.VERSION.SDK_INT_FULL,
            codename = Build.VERSION.CODENAME,
            release = Build.VERSION.RELEASE,
            lastVersionCode = getApiAndDesserts(kClass).lastOrNull()
        )
    }
}

/**
 * Pure derivation of [AndroidInfo] from plain runtime values, so callers (tests included)
 * reach any branch without touching `Build.*` or reflection:
 * - `sdkInt` / `runtimeSdkIntFull` / `codename` / `release` come from `Build.VERSION.SDK_INT`,
 *   `SDK_INT_FULL`, `CODENAME`, `RELEASE`;
 * - `lastVersionCode` is the highest version code found by reflection, null when it fails.
 */
internal fun deriveAndroidInfo(
    sdkInt: Int,
    runtimeSdkIntFull: Int,
    codename: String,
    release: String,
    lastVersionCode: VersionCode?
): AndroidInfo {
    var api = sdkInt
    var apiFull: String
    var version = release

    if (codename == CODENAME_RELEASE) {
        apiFull = api.toString() // "36"

        if (sdkInt >= Build.VERSION_CODES.BAKLAVA) {
            val suffix = ".${Build.getMinorSdkVersion(runtimeSdkIntFull)}" // ".1"
            apiFull += suffix // "36.1"
        }
    } else {
        if (sdkInt < Build.VERSION_CODES.VANILLA_ICE_CREAM) { // MinSupport ~ Android 15 Preview
            api++ // 34 → 35
            apiFull = api.toString() // "34" → "35"
            version.toIntOrNull()?.let {
                version = (it + 1).toString() // "14" → "15"
            }
        } else {
            val sdkIntFull = lastVersionCode?.api
                ?: runtimeSdkIntFull
            api = Build.getMajorSdkVersion(sdkIntFull)
            val minorFromRuntime = Build.getMinorSdkVersion(sdkIntFull)

            val suffix = ".$minorFromRuntime" // ".1"
            apiFull = "$api$suffix" // "36.1"
        }
    }

    return AndroidInfo(api, apiFull, version, lastVersionCode?.dessert) // Baklava 1
}

private fun getApiAndDesserts(kClass: KClass<*>): List<VersionCode> {
    val fields = try {
        kClass.java.fields
    } catch (e: Exception) {
        Log.w(kClass.simpleName, "Failed to get ${kClass::simpleName} fields. ${e.fullMessage}")
        emptyArray()
    }
    return fields.mapNotNull {
        try {
            it.isAccessible = true

            val name = it.name
            if (name != Build.VERSION_CODES::CUR_DEVELOPMENT.name) {
                VersionCode(it.getInt(null), name.toPascalCase())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(kClass.simpleName, "Failed to get $it. ${e.fullMessage}")
            null
        }
    }.sortedBy(VersionCode::api)
}
