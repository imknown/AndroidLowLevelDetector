package net.imknown.android.forefrontinfo.ui.prop

import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BasePureListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository
import kotlin.reflect.KClass

class PropViewModel(
    private val propRepository: PropRepository,
    private val savedStateHandle: SavedStateHandle
) : BasePureListViewModel() {

    companion object {
        private const val UNIX_LIKE_NEWLINE_ORIGIN = "\\n"

        val MY_REPOSITORY_KEY = object : CreationExtras.Key<PropRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as PropRepository
                val savedStateHandle = createSavedStateHandle()
                PropViewModel(repository, savedStateHandle)
            }
        }
    }

    override fun collectModels() {
        val tempModels = ArrayList<MyModel>()

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                getSystemProp(tempModels)

                getSettings(tempModels, Settings.System::class)
                getSettings(tempModels, Settings.Secure::class)
                getSettings(tempModels, Settings.Global::class)

                getBuildProp(tempModels)
            }

            if (tempModels.isEmpty()) {
                showError(R.string.lld_json_detect_failed, Exception())
            } else {
                setModels(tempModels)
            }
        }
    }

    private fun getSystemProp(tempModels: ArrayList<MyModel>) {
        try {
            propRepository.getSystemPropOrThrow().forEach {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun <T : Settings.NameValueTable> getSettings(
        tempModels: ArrayList<MyModel>, subSettingsKClass: KClass<T>
    ) {
        try {
            propRepository.getSettingsOrThrow(subSettingsKClass).forEach {
                var key = it
                val value = propRepository.getStringOrNullOrThrow(
                    subSettingsKClass, MyApplication.instance.contentResolver, key
                ) ?: MyApplication.getMyString(R.string.build_not_filled)
                key = "${subSettingsKClass.qualifiedName}.$key"
                tempModels.add(MyModel(key, value))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBuildProp(tempModels: ArrayList<MyModel>) {
        var temp = ""
        propRepository.getBuildProp().output.forEach {
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