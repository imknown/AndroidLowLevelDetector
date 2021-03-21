package net.imknown.android.forefrontinfo.ui.common

import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.property.getBooleanPropertyByCondition
import net.imknown.android.forefrontinfo.base.property.getStringPropertyByCondition

fun getStringProperty(
    key: String, condition: Boolean = true
): String {
    val notSupport = MyApplication.getMyString(R.string.result_not_supported)
    return if (
        isAtLeastStableAndroid6()
        || key.length < 31 // Avoid Android 5.0 crash: "key.length > 31"
    ) {
        getStringPropertyByCondition(
            key,
            condition,
            MyApplication.getMyString(R.string.build_not_filled),
            notSupport
        )
    } else {
        notSupport
    }
}

fun getBooleanProperty(key: String, condition: Boolean = true) =
    getBooleanPropertyByCondition(key, condition)