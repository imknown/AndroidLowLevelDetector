package net.imknown.android.forefrontinfo.ui.common

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import net.imknown.android.forefrontinfo.ui.home.model.Lld

private const val CODENAME_RELEASE = "REL"

fun isAtLeastStableAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
fun isAtLeastStableAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isAtLeastStableAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isAtLeastStableAndroid8P1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
fun isAtLeastStableAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
fun isAtLeastStableAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
fun isAtLeastStableAndroid11() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
fun isAtLeastStableAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
fun isAtLeastStableAndroid13() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
// fun isAtLeastStableAndroid14() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
// fun isAtLeastStableAndroid15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
fun isAtLeastStableAndroid16() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()

fun isLatestStableAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid()
        && getAndroidVersionName() >= lld.android.preview.name[0].toString()

fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

/** For minSdk ≥ Android 11, use [Build.VERSION.RELEASE_OR_CODENAME] (`ro.build.version.release_or_codename`) */
fun getAndroidVersionName(): String = if (isStableAndroid()) {
    Build.VERSION.RELEASE
} else {
    if (isAtLeastStableAndroid13()) {
        "${Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY}, "
    } else {
        ""
    } + Build.VERSION.CODENAME
}

fun Context.isGoEdition() = isAtLeastStableAndroid8P1() && isLowRamDevice()

private fun Context.isLowRamDevice() = ContextCompat.getSystemService(
    this, ActivityManager::class.java
)?.isLowRamDevice == true

fun getAndroidApiLevel() = if (isStableAndroid()) {
    Build.VERSION.SDK_INT
} else {
    Build.VERSION_CODES.CUR_DEVELOPMENT
}

//fun Context.getAndroidApiLevelDynamic() = packageManager.getApplicationInfo(
//    "android", 0
//).targetSdkVersion