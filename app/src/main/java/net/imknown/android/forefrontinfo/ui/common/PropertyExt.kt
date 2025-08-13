package net.imknown.android.forefrontinfo.ui.common

import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.property.PropertyManager

fun getStringProperty(
    key: String, condition: Boolean = true
): String {
    val notSupport = MyApplication.getMyString(R.string.result_not_supported)
    return if (
        isAtLeastStableAndroid8()
        || key.length <= 31 // Avoid Android 5.0 ~ 7.1.2 crash: "key.length > 31"
    ) {
        if (condition) {
            PropertyManager.instance.getString(key, MyApplication.getMyString(R.string.build_not_filled))
        } else {
            notSupport
        }
    } else {
        notSupport
    }
}

fun getBooleanProperty(key: String, condition: Boolean = true) = if (condition) {
    PropertyManager.instance.getBoolean(key, false)
} else {
    false
}