package net.imknown.android.forefrontinfo.ui.common

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.Locale

const val CODENAME_NONE = ""
const val CODENAME_CANARY = "CANARY"
const val CODENAME_RELEASE = "REL"

fun String.toPascalCase(): String {
    return split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { char -> char.titlecase(Locale.US) }
    }
}

// region [Stable/Preview]
fun isStableAndroid() = Build.VERSION.CODENAME == CODENAME_RELEASE
fun isPreviewAndroid() = !isStableAndroid()
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
