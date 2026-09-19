package net.imknown.android.forefrontinfo.ui.prop

import android.provider.Settings
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

class PropViewModel(
    private val propRepository: PropRepository
) : BaseListViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PropViewModel(PropRepository(PropertiesDataSource(), SettingsDataSource()))
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