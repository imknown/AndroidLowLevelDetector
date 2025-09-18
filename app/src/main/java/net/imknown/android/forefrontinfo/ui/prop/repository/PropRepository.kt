package net.imknown.android.forefrontinfo.ui.prop.repository

import android.provider.Settings
import android.util.Log
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.toPropMyModel
import net.imknown.android.forefrontinfo.ui.base.list.toTranslatedDetailMyModel
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import kotlin.reflect.KClass
import android.R as androidR

class PropRepository(
    private val propertiesDataSource: PropertiesDataSource,
    private val settingsDataSource: SettingsDataSource,
) {
    fun getSystemProp(): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        val pairs = try {
            propertiesDataSource.getSystemPropOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            return tempModels
        }

        pairs.forEach { (titleOrNull, detailOrNull) ->
            val title = titleOrNull.toString()
            val detail = if (detailOrNull == System.lineSeparator()) {
                PropertiesDataSource.UNIX_LIKE_NEWLINE_ORIGIN
            } else {
                detailOrNull?.toString()
            }
            tempModels += toTranslatedDetailMyModel(title, detail)
        }

        return tempModels
    }

    fun <T : Settings.NameValueTable> getSettings(subSettingsKClass: KClass<T>): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        val list = try {
            settingsDataSource.getSettingsOrThrow(subSettingsKClass)
        } catch (e: Exception) {
            e.printStackTrace()
            return tempModels
        }

        list.forEach {
            val key = "${subSettingsKClass.qualifiedName}.$it"
            val value = try {
                settingsDataSource.getStringOrNullOrThrow(
                    subSettingsKClass, MyApplication.instance.contentResolver, it
                )
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "$key: ${e.fullMessage}")
                MyApplication.getMyString(androidR.string.unknownName)
            }
            tempModels.add(toTranslatedDetailMyModel(key, value))
        }

        return tempModels
    }

    fun getBuildProp(): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        var temp = ""
        propertiesDataSource.getBuildProp().output.forEach {
            if (it.startsWith("[") && it.endsWith("]")) {
                tempModels += toPropMyModel(rawProp = it)
            } else {
                temp += "$it\n"

                if (it.endsWith("]")) {
                    tempModels += toPropMyModel(rawProp = temp)

                    temp = ""
                }
            }
        }

        return tempModels
    }
}