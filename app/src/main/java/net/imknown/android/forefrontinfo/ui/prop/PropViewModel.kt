package net.imknown.android.forefrontinfo.ui.prop

import android.content.ContentResolver
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BasePureListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import java.util.Locale
import java.util.Properties
import kotlin.reflect.KClass

class PropViewModel : BasePureListViewModel() {

    companion object {
        private const val CMD_GETPROP = "getprop"

        private const val UNIX_LIKE_NEWLINE_ORIGIN = "\\n"
    }

    override fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val tempModels = ArrayList<MyModel>()

            getSystemProp(tempModels)
            getSettings(tempModels, Settings.System::class)
            getSettings(tempModels, Settings.Secure::class)
            getSettings(tempModels, Settings.Global::class)
            getBuildProp(tempModels)

            setModels(tempModels)
        } catch (e: Exception) {
            showError(R.string.lld_json_detect_failed, e)
        }
    }

    private fun getSystemProp(tempModels: ArrayList<MyModel>) {
        val systemProperties = System.getProperties()
        val defaultsProperties = Properties::class.java
            .getDeclaredField("defaults")
            .also { it.isAccessible = true }
            .get(systemProperties) as Properties

        (defaultsProperties + systemProperties)
            .toList()
            .sortedBy { it.first.toString() }
            .forEach {
                add(
                    tempModels,
                    it.first.toString(),
                    if (it.second == System.lineSeparator()) {
                        UNIX_LIKE_NEWLINE_ORIGIN
                    } else {
                        it.second.toString()
                    }
                )
            }
    }

    private fun <T> getSettings(tempModels: ArrayList<MyModel>, subSettingsKClass: KClass<T>)
            where T : Settings.NameValueTable {
        subSettingsKClass.java.declaredFields
            .filter {
                it.isAccessible = true
                it.type == String::class.java
            }.map { it.get(null) as String }
            .sortedBy { it.uppercase(Locale.US) }
            .forEach {
                var key = it
                try {
                    val value = subSettingsKClass.java.getDeclaredMethod(
                        "getString",
                        ContentResolver::class.java,
                        String::class.java
                    ).invoke(null, MyApplication.instance.contentResolver, key) as? String
                        ?: MyApplication.getMyString(R.string.build_not_filled)
                    key = "${subSettingsKClass.qualifiedName}.$key"
                    tempModels.add(MyModel(key, value))
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, e.cause?.message.toString())
                }
            }
    }

    private fun getBuildProp(tempModels: ArrayList<MyModel>) {
        var temp = ""
        sh(CMD_GETPROP).output.forEach {
            if (it.startsWith("[") && it.endsWith("]")) {
                addRawProp(tempModels, it)
            } else {
                temp += "$it\n"

                if (it.endsWith("]")) {
                    addRawProp(tempModels, temp)

                    temp = ""
                }
            }
        }
    }

    private fun addRawProp(tempModels: ArrayList<MyModel>, text: String) {
        val result = text.split(": ")
        add(tempModels, removeSquareBrackets(result[0]), removeSquareBrackets(result[1]))
    }

    private fun removeSquareBrackets(text: String) =
        text.substringAfter("[").substringBefore(']').trimIndent()
}