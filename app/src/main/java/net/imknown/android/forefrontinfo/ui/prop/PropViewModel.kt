package net.imknown.android.forefrontinfo.ui.prop

import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

class PropViewModel(
    private val propRepository: PropRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseListViewModel() {

    companion object {
        val MY_REPOSITORY_KEY = object : CreationExtras.Key<PropRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as PropRepository
                val savedStateHandle = createSavedStateHandle()
                PropViewModel(repository, savedStateHandle)
            }
        }
    }

    override suspend fun collectModels(): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        withContext(Dispatchers.Default) {
            tempModels += propRepository.getSystemProp()

            tempModels += propRepository.getSettings(Settings.System::class)
            tempModels += propRepository.getSettings(Settings.Secure::class)
            tempModels += propRepository.getSettings(Settings.Global::class)

            tempModels += propRepository.getBuildProp()
        }

        return tempModels
    }
}