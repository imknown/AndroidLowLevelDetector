package net.imknown.android.forefrontinfo.ui.common

import android.util.Log
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.property.PropertyManager

fun getStringProperty(
    key: String, condition: Boolean = true
): String {
    val notSupport = MyApplication.getMyString(R.string.result_not_supported)
    return if (condition) {
        try {
            val default = MyApplication.getMyString(R.string.build_not_filled)
            PropertyManager.instance.getStringOrThrow(key, default)
        } catch (e: Exception) {
            Log.w("getStringProperty", "$key: ${e.fullMessage}")
            notSupport
        }
    } else {
        notSupport
    }
}

fun getBooleanProperty(key: String, condition: Boolean = true) = if (condition) {
    try {
        PropertyManager.instance.getBooleanOrThrow(key, false)
    } catch (e: Exception) {
        Log.w("getBooleanProperty", "$key: ${e.fullMessage}")
        false
    }
} else {
    false
}