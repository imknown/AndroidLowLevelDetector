package net.imknown.android.forefrontinfo.ui.prop

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.ui.base.BasePureListViewModel
import net.imknown.android.forefrontinfo.ui.base.MyModel
import java.util.*

class PropViewModel : BasePureListViewModel() {

    companion object {
        private const val CMD_GETPROP = "getprop"

        private const val UNIX_LIKE_NEWLINE_ORIGIN = "\\n"
    }

    override fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
        val tempModels = ArrayList<MyModel>()

        getSystemProp(tempModels)
        getBuildProp(tempModels)

        setModels(tempModels)
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