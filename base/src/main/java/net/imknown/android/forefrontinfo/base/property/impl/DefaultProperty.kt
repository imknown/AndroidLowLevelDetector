package net.imknown.android.forefrontinfo.base.property.impl

import android.annotation.SuppressLint
import net.imknown.android.forefrontinfo.base.property.IProperty

@SuppressLint("PrivateApi")
object DefaultProperty : IProperty {
    private val systemPropertiesClass = Class.forName("android.os.SystemProperties")

    override fun getString(key: String, default: String): String =
        systemPropertiesClass.getDeclaredMethod(
            "get", String::class.java, String::class.java
        ).invoke(null, key, default) as String

    override fun getBoolean(key: String, default: Boolean): Boolean =
        systemPropertiesClass.getDeclaredMethod(
            "getBoolean", String::class.java, Boolean::class.java
        ).invoke(null, key, default) as Boolean
}