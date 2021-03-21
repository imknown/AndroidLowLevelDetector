package net.imknown.android.forefrontinfo.ui.prop.repository

import android.content.ContentResolver
import android.provider.Settings
import net.imknown.android.forefrontinfo.ui.base.IRepository
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import kotlin.reflect.KClass

class PropRepository(
    private val propertiesDataSource: PropertiesDataSource,
    private val settingsDataSource: SettingsDataSource,
) : IRepository {
    fun getSystemPropOrThrow() = propertiesDataSource.getSystemPropOrThrow()

    fun <T : Settings.NameValueTable> getSettingsOrThrow(klass: KClass<T>) =
        settingsDataSource.getSettingsOrThrow(klass)

    fun <T : Settings.NameValueTable> getStringOrNullOrThrow(
        subSettingsKClass: KClass<T>, contentResolver: ContentResolver, key: String
    ) = settingsDataSource.getStringOrNullOrThrow(subSettingsKClass, contentResolver, key)

    fun getBuildProp() = propertiesDataSource.getBuildProp()
}