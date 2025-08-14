package net.imknown.android.forefrontinfo.base.property

interface IProperty {
    fun getStringOrThrow(key: String, default: String): String
    fun getBooleanOrThrow(key: String, default: Boolean): Boolean
}