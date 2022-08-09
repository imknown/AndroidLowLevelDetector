package net.imknown.android.forefrontinfo.base.property

class PropertyManager(property: IProperty) : IProperty by property {
    companion object {
        lateinit var instance: PropertyManager
    }
}