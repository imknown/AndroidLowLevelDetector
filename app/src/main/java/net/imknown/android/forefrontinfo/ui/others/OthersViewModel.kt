package net.imknown.android.forefrontinfo.ui.others

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid12
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository

// Metro wiring: the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseListViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class OthersViewModel(
    private val othersRepository: OthersRepository
) : BaseListViewModel() {

    override suspend fun collectModels(): List<MyModel> {
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
            tempModels += othersRepository.getBaseOs()
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
            tempModels += othersRepository.getPreviewSdkInt()
            tempModels += othersRepository.getDefaultUserAgent(MyApplication.instance)
            tempModels += othersRepository.getKernelVersion()
            // endregion [ROM]

            // region [Others]
            tempModels += othersRepository.getBootloader()
            tempModels += othersRepository.getRadioVersionOrNull()
            // endregion [Others]
        }

        return tempModels
    }
}