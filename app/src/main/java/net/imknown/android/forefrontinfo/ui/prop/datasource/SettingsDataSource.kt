package net.imknown.android.forefrontinfo.ui.prop.datasource

import android.content.ContentResolver
import android.provider.Settings
import java.util.Locale
import kotlin.reflect.KClass

class SettingsDataSource {
    fun <T : Settings.NameValueTable> getSettingsOrThrow(subSettingsKClass: KClass<T>): List<String> {
        return subSettingsKClass.java.declaredFields
            .filter {
                it.isAccessible = true
                it.type == String::class.java
            }.map { it.get(null) as String }
            .sortedBy { it.uppercase(Locale.US) }
    }

    fun <T : Settings.NameValueTable> getStringOrNullOrThrow(
        subSettingsKClass: KClass<T>, contentResolver: ContentResolver, key: String
    ): String? {
        return subSettingsKClass.java.getDeclaredMethod(
            "getString", ContentResolver::class.java, String::class.java
        ).invoke(null, contentResolver, key) as? String
    }
}