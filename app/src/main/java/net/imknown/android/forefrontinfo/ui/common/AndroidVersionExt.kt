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
import java.util.Locale
import kotlin.reflect.KClass

const val CODENAME_NONE = ""
const val CODENAME_CANARY = "CANARY"
const val CODENAME_RELEASE = "REL"

/** See: [Build.VERSION_CODES_FULL].SDK_INT_MULTIPLIER */
private const val SDK_INT_MULTIPLIER = 1_00000

data class ApiAndDessert(val api: Int, val dessert: String)

/** See: [Build.VERSION].RESOURCES_SDK_INT */
private fun getApiAndDesserts(kClass: KClass<*>): List<ApiAndDessert> {
    val fields = try {
        kClass.java.fields
    } catch (e: Exception) {
        Log.w(kClass.simpleName, "Failed to get ${kClass::simpleName} fields. ${e.fullMessage}")
        emptyArray()
    }
    return fields.mapNotNull {
        try {
            it.isAccessible = true

            val api = it.getInt(null)
            if (api != Build.VERSION_CODES.CUR_DEVELOPMENT) {
                ApiAndDessert(api, it.name.toPascalCase())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(kClass.simpleName, "Failed to get $it. ${e.fullMessage}")
            null
        }
    }.sortedBy(ApiAndDessert::api)
}

private fun String.toPascalCase(): String {
    return split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { char -> char.titlecase(Locale.US) }
    }
}

private fun getLatestApiAndDessertOrNull(kClass: KClass<*>) = getApiAndDesserts(kClass).lastOrNull()

private val lastestApiAndDessert by lazy {
    getLatestApiAndDessertOrNull(Build.VERSION_CODES::class)
}

val lastestApiFullAndDessert by lazy {
    if (isAtLeastAndroid16()) {
        getLatestApiAndDessertOrNull(Build.VERSION_CODES_FULL::class)
    } else {
        lastestApiAndDessert
    }
}

/** E.g.: 36 */
val sdkInt: Int by lazy {
    if (isStableAndroid()) {
        Build.VERSION.SDK_INT
    } else {
        lastestApiAndDessert?.api
            ?: Build.VERSION.SDK_INT
    }
}

/** E.g.: 3600001 */
private val sdkIntFull: Int by lazy {
    if (isAtLeastAndroid16()) {
        if (isStableAndroid()) {
            Build.VERSION.SDK_INT_FULL
        } else {
            lastestApiFullAndDessert?.api
                ?: Build.VERSION.SDK_INT_FULL
        }
    } else {
        sdkInt * SDK_INT_MULTIPLIER
    }
}

/** E.g.: "35", "36.1" */
val sdkFull: String by lazy {
    if (isAtLeastAndroid16()) {
        "$sdkInt.${Build.getMinorSdkVersion(sdkIntFull)}"
    } else {
        sdkInt.toString()
    }
}

fun isAtLeastAndroid6() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || sdkInt >= Build.VERSION_CODES.M
fun isAtLeastAndroid7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || sdkInt >= Build.VERSION_CODES.N
fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || sdkInt >= Build.VERSION_CODES.O
fun isAtLeastAndroid8p1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || sdkInt >= Build.VERSION_CODES.O_MR1
fun isAtLeastAndroid9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || sdkInt >= Build.VERSION_CODES.P
fun isAtLeastAndroid10() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || sdkInt >= Build.VERSION_CODES.Q
fun isAtLeastAndroid11() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || sdkInt >= Build.VERSION_CODES.R
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
fun isAtLeastAndroid13() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || sdkInt >= Build.VERSION_CODES.TIRAMISU
// fun isAtLeastAndroid14() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
// fun isAtLeastAndroid15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM
fun isAtLeastAndroid16() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA || sdkInt >= Build.VERSION_CODES.BAKLAVA

fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()

fun isLatestStableAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid()
        && sdkFull >= lld.android.preview.apiFull

fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid()
        && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

fun Context.isGoEdition() = isAtLeastAndroid8p1() && isLowRamDevice()

private fun Context.isLowRamDevice() = ContextCompat.getSystemService(
    this, ActivityManager::class.java
)?.isLowRamDevice == true

@RequiresApi(Build.VERSION_CODES.R)
fun getSdkExtension(extension: Int): Int = SdkExtensions.getExtensionVersion(extension)

//fun Context.getAndroidApiLevelDynamic() = packageManager.getApplicationInfo(
//    "android", 0
//).targetSdkVersion