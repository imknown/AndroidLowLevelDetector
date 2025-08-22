package net.imknown.android.forefrontinfo.ui.common

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import kotlin.reflect.KClass

private const val CODENAME_RELEASE = "REL"

/** See: [Build.VERSION].RESOURCES_SDK_INT */
private fun listCodes(kClass: KClass<*>): List<Pair<String?, Int>> {
    val fields = try {
        kClass.java.fields
    } catch (e: Exception) {
        Log.w(kClass.simpleName, "Failed to get ${kClass::simpleName} fields. ${e.fullMessage}")
        emptyArray()
    }
    return fields.mapNotNull {
        try {
            it.isAccessible = true

            val value = it.getInt(null)
            if (value != Build.VERSION_CODES.CUR_DEVELOPMENT) {
                Pair(it.name, value)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(kClass.simpleName, "Failed to get $it. ${e.fullMessage}")
            null
        }
    }.sortedBy { it.second }
}

private fun latestApiOrNull(kClass: KClass<*>) = listCodes(kClass).lastOrNull()?.second

val sdkInt by lazy {
    if (isStableAndroid()) {
        Build.VERSION.SDK_INT
    } else {
        latestApiOrNull(Build.VERSION_CODES::class)
            ?: Build.VERSION.SDK_INT
    }
}

val sdkIntFull by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        if (isStableAndroid()) {
            Build.VERSION.SDK_INT_FULL
        } else {
            latestApiOrNull(Build.VERSION_CODES_FULL::class)
                ?: Build.VERSION.SDK_INT_FULL
        }
    } else {
        sdkInt
    }
}

fun isAtLeastStableAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || sdkInt >= Build.VERSION_CODES.M
fun isAtLeastStableAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || sdkInt >= Build.VERSION_CODES.N
fun isAtLeastStableAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || sdkInt >= Build.VERSION_CODES.O
fun isAtLeastStableAndroid8P1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || sdkInt >= Build.VERSION_CODES.O_MR1
fun isAtLeastStableAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || sdkInt >= Build.VERSION_CODES.P
fun isAtLeastStableAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || sdkInt >= Build.VERSION_CODES.Q
fun isAtLeastStableAndroid11() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || sdkInt >= Build.VERSION_CODES.R
fun isAtLeastStableAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
fun isAtLeastStableAndroid13() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || sdkInt >= Build.VERSION_CODES.TIRAMISU
// fun isAtLeastStableAndroid14() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
// fun isAtLeastStableAndroid15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM
fun isAtLeastStableAndroid16() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA || sdkInt >= Build.VERSION_CODES.BAKLAVA

fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()

fun isLatestStableAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid()
        && sdkInt >= lld.android.preview.api.toInt()

fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

/**
 * [Build.VERSION.RELEASE_OR_CODENAME]: Android 11+
 * [Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY]: Android 13+
 * [Build.VERSION.CODENAME]
 */
fun getAndroidDessertPreview(): String = if (isAtLeastStableAndroid13()) {
    val codename = Build.VERSION.CODENAME
    codename + if (codename != Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY) {
        ", " + Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
    } else {
        ""
    }
} else {
    Build.VERSION.CODENAME
}

fun Context.isGoEdition() = isAtLeastStableAndroid8P1() && isLowRamDevice()

private fun Context.isLowRamDevice() = ContextCompat.getSystemService(
    this, ActivityManager::class.java
)?.isLowRamDevice == true

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
fun getAndroidApiLevelMinor(sdkIntFull: Int): Int = Build.getMinorSdkVersion(sdkIntFull)

@RequiresApi(Build.VERSION_CODES.R)
fun getSdkExtension(extension: Int): Int = SdkExtensions.getExtensionVersion(extension)

//fun Context.getAndroidApiLevelDynamic() = packageManager.getApplicationInfo(
//    "android", 0
//).targetSdkVersion