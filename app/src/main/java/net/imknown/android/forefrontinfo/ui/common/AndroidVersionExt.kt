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
    val kClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        Build.VERSION_CODES_FULL::class
    } else {
        Build.VERSION_CODES::class
    }
    val versionCodes = getApiAndDesserts(kClass)
    val versionCodeLast = versionCodes.lastOrNull()

    var api = Build.VERSION.SDK_INT
    var apiFull: String
    var version = Build.VERSION.RELEASE

    if (isStableAndroid()) {
        apiFull = api.toString() // "36"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val suffix = ".$minor" // ".1"
            apiFull += suffix // "36.1"
        }
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) { // MinSupport ~ Android 15 Preview
            api++ // 34 → 35
            apiFull = api.toString() // "34" → "35"
            version.toIntOrNull()?.let {
                version = (it + 1).toString() // "14" → "15"
            }
        } else {
            val sdkIntFull = versionCodeLast?.api
                ?: Build.VERSION.SDK_INT_FULL
            api = Build.getMajorSdkVersion(sdkIntFull)
            val minorFromRuntime = Build.getMinorSdkVersion(sdkIntFull)

            val suffix = ".$minorFromRuntime" // ".1"
            apiFull = "$api$suffix" // "36.1"
        }
    }

    myAndroid.api = api
    myAndroid.apiFull = apiFull
    myAndroid.version = version

    val dessert = versionCodeLast?.dessert // Baklava 1
    myAndroid.dessert = dessert
}
// endregion [MyAndroid]

// region [Stable/Preview]
fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()

fun isLatestStableAndroid(lld: Lld) = isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()
fun isLatestPreviewAndroid(lld: Lld) = isPreviewAndroid() && myAndroid.apiFull >= lld.android.preview.apiFull
fun isSupportedByUpstreamAndroid(lld: Lld) = isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()
// endregion [Stable/Preview]

fun Context.isGoEdition() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && isLowRamDevice()

private fun Context.isLowRamDevice() = ContextCompat.getSystemService(
    this, ActivityManager::class.java
)?.isLowRamDevice == true

@RequiresApi(Build.VERSION_CODES.R)
fun getSdkExtension(extension: Int): Int = SdkExtensions.getExtensionVersion(extension)

//fun Context.getAndroidApiLevelDynamic() = packageManager.getApplicationInfo(
//    "android", 0
//).targetSdkVersion