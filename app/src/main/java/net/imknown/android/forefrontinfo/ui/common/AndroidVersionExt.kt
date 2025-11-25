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
private const val CODENAME_RELEASE = "REL"

// region [MyAndroid]
private data class ApiAndDessert(val api: Int, val dessert: String)

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

            val name = it.name
            if (name != Build.VERSION_CODES::CUR_DEVELOPMENT.name) {
                ApiAndDessert(it.getInt(null), name.toPascalCase())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(kClass.simpleName, "Failed to get $it. ${e.fullMessage}")
            null
        }
    }.sortedBy(ApiAndDessert::api)
}

fun String.toPascalCase(): String {
    return split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { char -> char.titlecase(Locale.US) }
    }
}

/** See: [Lld.Androids.Android] */
class MyAndroid(
    var api: Int, // 36
    var apiFull: String, // "35", "36.0", "36.1"
    var version: String, // "15", "16", "16.1 (QPR2)"
    var dessert: String? = null // "Baklava 1"
)

private val minor by lazy { Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL) }

val myAndroid = MyAndroid(
    Build.VERSION.SDK_INT, "${Build.VERSION.SDK_INT}.$minor", Build.VERSION.RELEASE
)

fun initMyAndroid() {
    var api = Build.VERSION.SDK_INT
    var apiFull: String
    var version = Build.VERSION.RELEASE

    if (isStableAndroid()) {
        apiFull = api.toString() // "36"

        if (isAtLeastAndroid16()) {
            val suffix = ".$minor" // ".1"
            apiFull += suffix // "36.1"
            if (minor != 0) {
                version += suffix // "16.1"
            }
        }
    } else {
        fun apiIncrease() {
            api++
        }

        fun versionIncrease() {
            version.toIntOrNull()?.let {
                version = (it + 1).toString()
            }
        }

        if (!isAtLeastAndroid15()) { // Android 6 Preview ~ Android 15 Preview
            apiIncrease() // 34 → 35
            apiFull = api.toString() // "34" → "35"
            versionIncrease() // "14" → "15"
        } else {
            if (api == Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 16.0 Preview
                apiIncrease() // 35 → 36
                apiFull = "$api.0" // "35" → "36.0"
                versionIncrease() // "15" → "16"
            } else { // Android 16.1 Preview and above
                if (minor != 0) {
                    apiIncrease() // 36.1 → 37
                }

                val minorReal = if (minor != 0) 0 else 1

                apiFull = "$api.$minorReal" // 36.0 → 36.1; 36.1 → 37.0

                if (minor != 0) {
                    versionIncrease() // "16.1" → "17"
                } else {
                    version += ".$minorReal" // "16" → "16.1"
                }
            }
        }
    }

    myAndroid.api = api
    myAndroid.apiFull = apiFull
    myAndroid.version = version

    val kClass = if (isAtLeastAndroid16()) {
        Build.VERSION_CODES_FULL::class
    } else {
        Build.VERSION_CODES::class
    }
    val versionCodes = getApiAndDesserts(kClass)
    val versionCodeLast = versionCodes.lastOrNull()
    val dessert = versionCodeLast?.dessert // Baklava 1
    myAndroid.dessert = dessert
}
// endregion [MyAndroid]

// region [IsAtLeast]
private val sdkInt get() = myAndroid.api

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
fun isAtLeastAndroid15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM || sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM
fun isAtLeastAndroid16() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA || sdkInt >= Build.VERSION_CODES.BAKLAVA
// endregion [IsAtLeast]

// region [Stable/Preview]
fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()

fun isLatestStableAndroid(lld: Lld) = isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()
fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid() && myAndroid.apiFull >= lld.android.preview.apiFull
fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
// endregion [Stable/Preview]

fun Context.isGoEdition() = isAtLeastAndroid8p1() && isLowRamDevice()

private fun Context.isLowRamDevice() = ContextCompat.getSystemService(
    this, ActivityManager::class.java
)?.isLowRamDevice == true

@RequiresApi(Build.VERSION_CODES.R)
fun getSdkExtension(extension: Int): Int = SdkExtensions.getExtensionVersion(extension)

//fun Context.getAndroidApiLevelDynamic() = packageManager.getApplicationInfo(
//    "android", 0
//).targetSdkVersion