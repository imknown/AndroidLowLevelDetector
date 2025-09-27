package net.imknown.android.forefrontinfo.ui.others

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
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid12
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid6
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository

class OthersViewModel(
    private val othersRepository: OthersRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseListViewModel() {

    companion object {
        val MY_REPOSITORY_KEY = object : CreationExtras.Key<OthersRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as OthersRepository
                val savedStateHandle = createSavedStateHandle()
                OthersViewModel(repository, savedStateHandle)
            }
        }
    }

    override fun collectModels() {
        viewModelScope.launch {
            val tempModels = mutableListOf<MyModel>()

            withContext(Dispatchers.Default) {
                // region [Basic]
                tempModels += othersRepository.getBrand()
                tempModels += othersRepository.getManufacturer()
                tempModels += othersRepository.getModel()
                tempModels += othersRepository.getDevice()
                tempModels += othersRepository.getProduct()
                tempModels += othersRepository.getHardware()
                tempModels += othersRepository.getBoard()

                if (isAtLeastAndroid12()) {
                    tempModels += othersRepository.getSocModel()
                    tempModels += othersRepository.getSocManufacturer()
                    tempModels += othersRepository.getSku()
                    tempModels += othersRepository.getVendorSku()
                    tempModels += othersRepository.getOdmSku()
                }
                // endregion [Basic]

                // region [Arch & ABI]
                // region [Binder]
                tempModels += othersRepository.getBinderStatus(ArchitectureDataSource.DRIVER_BINDER)
                // endregion [Binder]

                // region [Process]
                tempModels += othersRepository.getProcessBit()
                tempModels += othersRepository.getArchitecture()
                // endregion [Process]

                // region [ABI]
                tempModels += othersRepository.getCpuAbi()
                tempModels += othersRepository.getPropertyCpuAbi()
                tempModels += othersRepository.getSupported32BitAbis()
                tempModels += othersRepository.getSupported64BitAbis()
                // endregion [ABI]
                // endregion [Arch & ABI]

                // region [ROM]
                tempModels += othersRepository.getUser()
                tempModels += othersRepository.getHost()
                tempModels += othersRepository.getTime()
                if (isAtLeastAndroid6()) {
                    tempModels += othersRepository.getBaseOs()
                }
                // region [Fingerprints]
                tempModels += othersRepository.getFingerprint()
                if (isAtLeastAndroid10()) {
                    tempModels += othersRepository.getPreviewSdkFingerprint()
                }
                tempModels += othersRepository.getPartitionFingerprints()
                // endregion [Fingerprints]
                tempModels += othersRepository.getId()
                tempModels += othersRepository.getDisplay()
                tempModels += othersRepository.getType()
                tempModels += othersRepository.getTags()
                tempModels += othersRepository.getIncremental()
                tempModels += othersRepository.getCodename()
                if (isAtLeastAndroid6()) {
                    tempModels += othersRepository.getPreviewSdkInt()
                }
                tempModels += othersRepository.getDefaultUserAgent(MyApplication.instance)
                tempModels += othersRepository.getKernelVersion()
                // endregion [ROM]

                // region [Others]
                tempModels += othersRepository.getBootloader()
                tempModels += othersRepository.getRadioVersionOrNull()
                // endregion [Others]
            }

            setModels(tempModels)
        }
    }
}