package net.imknown.android.forefrontinfo.base.property

interface IProperty {
    fun getString(key: String, default: String): String
    fun getBoolean(key: String, default: Boolean): Boolean
}