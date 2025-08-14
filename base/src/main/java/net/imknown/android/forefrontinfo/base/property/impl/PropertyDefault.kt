package net.imknown.android.forefrontinfo.base.property.impl

import android.annotation.SuppressLint
import net.imknown.android.forefrontinfo.base.property.IProperty

@SuppressLint("PrivateApi")
object PropertyDefault : IProperty {
    private val systemPropertiesClass = Class.forName("android.os.SystemProperties")

    override fun getStringOrThrow(key: String, default: String): String =
        systemPropertiesClass.getDeclaredMethod(
            "get", String::class.java, String::class.java
        ).invoke(null, key, default) as String

    override fun getBooleanOrThrow(key: String, default: Boolean): Boolean =
        systemPropertiesClass.getDeclaredMethod(
            "getBoolean", String::class.java, Boolean::class.java
        ).invoke(null, key, default) as Boolean
}