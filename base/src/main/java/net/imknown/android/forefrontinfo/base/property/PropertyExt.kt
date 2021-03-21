package net.imknown.android.forefrontinfo.base.property

fun getStringPropertyByCondition(
    key: String, condition: Boolean,
    defaultString: String, elseString: String
): String = if (condition) {
    PropertyManager.instance.getString(key, defaultString)
} else {
    elseString
}

fun getBooleanPropertyByCondition(key: String, condition: Boolean) = if (condition) {
    PropertyManager.instance.getBoolean(key, false)
} else {
    false
}