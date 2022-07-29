package net.imknown.android.forefrontinfo.base.property.impl

import net.imknown.android.forefrontinfo.base.property.IProperty
import rikka.shizuku.ShizukuSystemProperties

object ShizukuProperty : IProperty {
    override fun getString(key: String, default: String): String =
        ShizukuSystemProperties.get(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        ShizukuSystemProperties.getBoolean(key, default)
}