package net.imknown.android.forefrontinfo.base.property

object PropertyManager {
    lateinit var property: IProperty

    fun getString(key: String, default: String) = property.getString(key, default)
    fun getBoolean(key: String, default: Boolean) = property.getBoolean(key, default)
}