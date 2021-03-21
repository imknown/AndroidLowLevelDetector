package net.imknown.android.forefrontinfo.ui.others

import android.util.Log
import androidx.annotation.StringRes
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
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.binderdetector.BinderDetector
import net.imknown.android.forefrontinfo.ui.base.isAtLeastStableAndroid10
import net.imknown.android.forefrontinfo.ui.base.isAtLeastStableAndroid12
import net.imknown.android.forefrontinfo.ui.base.isAtLeastStableAndroid6
import net.imknown.android.forefrontinfo.ui.base.list.BasePureListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid10
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid12
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid6
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository

class OthersViewModel(
    private val othersRepository: OthersRepository,
    private val savedStateHandle: SavedStateHandle
) : BasePureListViewModel() {

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
        val tempModels = ArrayList<MyModel>()

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                // region [Basic]
                add(tempModels, MyApplication.getMyString(R.string.build_brand), othersRepository.getBrand())
                add(tempModels, MyApplication.getMyString(R.string.build_manufacturer), othersRepository.getManufacturer())
                add(tempModels, MyApplication.getMyString(R.string.build_model), othersRepository.getModel())
                add(tempModels, MyApplication.getMyString(R.string.build_device), othersRepository.getDevice())
                add(tempModels, MyApplication.getMyString(R.string.build_product), othersRepository.getProduct())
                add(tempModels, MyApplication.getMyString(R.string.build_hardware), othersRepository.getHardware())
                add(tempModels, MyApplication.getMyString(R.string.build_board), othersRepository.getBoard())

                if (isAtLeastStableAndroid12()) {
                    add(tempModels, MyApplication.getMyString(R.string.build_soc_model), othersRepository.getSocModel())
                    add(tempModels, MyApplication.getMyString(R.string.build_soc_manufacturer), othersRepository.getSocManufacturer())
                    add(tempModels, MyApplication.getMyString(R.string.build_hardware_sku), othersRepository.getSku())
                    add(tempModels, MyApplication.getMyString(R.string.build_vendor_sku),othersRepository.getVendorSku())
                    add(tempModels, MyApplication.getMyString(R.string.build_odm_hardware_sku), othersRepository.getOdmSku())
                }
                // endregion [Basic]

                // region [Arch & ABI]
                // region [Binder]
                detectBinderStatus(tempModels, ArchitectureDataSource.DRIVER_BINDER, R.string.binder_status)
                // endregion [Binder]

                // region [Process]
                add(tempModels, MyApplication.getMyString(R.string.current_process_bit), getProcessBit())
                add(tempModels, MyApplication.getMyString(R.string.os_arch), othersRepository.getArchitectureOrNullOrThrow())
                // endregion [Process]

                // region [ABI]
                add(tempModels, MyApplication.getMyString(R.string.build_cpu_abi), othersRepository.getCpuAbi())
                add(tempModels, MyApplication.getMyString(R.string.current_system_abi), othersRepository.getPropertyCpuAbi())
                add(tempModels, MyApplication.getMyString(R.string.build_supported_32_bit_abis), othersRepository.getSupported32BitAbis().joinToString())
                val supported64BitAbis = othersRepository.getSupported64BitAbis().joinToString().takeIf { it.isNotEmpty() }
                    ?: MyApplication.getMyString(R.string.result_not_supported)
                add(tempModels, MyApplication.getMyString(R.string.build_supported_64_bit_abis), supported64BitAbis)
                // endregion [ABI]
                // endregion [Arch & ABI]

                // region [ROM]
                add(tempModels, MyApplication.getMyString(R.string.build_user), othersRepository.getUser())
                add(tempModels, MyApplication.getMyString(R.string.build_host), othersRepository.getHost())
                val time = othersRepository.getTime().formatToLocalZonedDatetimeString()
                add(tempModels, MyApplication.getMyString(R.string.build_time), time)
                if (isAtLeastStableAndroid6()) {
                    add(tempModels, MyApplication.getMyString(R.string.build_base_os), othersRepository.getBaseOs())
                }
                addFingerprints(tempModels)
                add(tempModels, MyApplication.getMyString(R.string.build_id), othersRepository.getId())
                add(tempModels, MyApplication.getMyString(R.string.build_display), othersRepository.getDisplay())
                add(tempModels, MyApplication.getMyString(R.string.build_type), othersRepository.getType())
                add(tempModels, MyApplication.getMyString(R.string.build_tags), othersRepository.getTags())
                add(tempModels, MyApplication.getMyString(R.string.build_incremental), othersRepository.getIncremental())
                add(tempModels, MyApplication.getMyString(R.string.build_codename), othersRepository.getCodename())
                if (isAtLeastStableAndroid6()) {
                    add(tempModels, MyApplication.getMyString(R.string.build_preview_sdk_int), othersRepository.getPreviewDdkInt().toString())
                }

                val userAgent = try {
                    othersRepository.getDefaultUserAgent(MyApplication.instance)
                } catch (e: Exception) {
                    e.printStackTrace()
                    MyApplication.getMyString(android.R.string.unknownName)
                }
                add(tempModels, MyApplication.getMyString(R.string.webview_user_agent), userAgent)

                // region [Kernel]
                var kernelFinal: String? = null
                val kernelVerbose = othersRepository.getKernelVersion()
                if (kernelVerbose.isSuccess) {
                    kernelFinal = kernelVerbose.output.getOrNull(0)
                } else {
                    val kernelAll = othersRepository.getKernelAll()
                    if (kernelAll.isSuccess) {
                        kernelFinal = kernelAll.output.getOrNull(0)
                    }
                }

                kernelFinal?.let {
                    add(tempModels, MyApplication.getMyString(R.string.linux), it)
                }
                // endregion [Kernel]
                // endregion [ROM]

                // region [Others]
                add(tempModels, MyApplication.getMyString(R.string.build_bootloader), othersRepository.getBootloader())
                add(tempModels, MyApplication.getMyString(R.string.build_radio), othersRepository.getRadioVersionOrNull())
                // endregion [Others]
            }

            setModels(tempModels)
        }

        try {

        } catch (e: Exception) {
            showError(R.string.lld_json_detect_failed, e)
        }
    }

    private fun addFingerprints(tempModels: ArrayList<MyModel>) {
        add(tempModels, MyApplication.getMyString(R.string.build_stock_fingerprint), othersRepository.getFingerprint())

        if (isAtLeastStableAndroid10()) {
            add(tempModels, MyApplication.getMyString(R.string.build_stock_preview_fingerprint), othersRepository.getPreviewSdkFingerprint())
        }

        addPartitionFingerprints(tempModels)
    }

    private fun addPartitionFingerprints(tempModels: ArrayList<MyModel>) {
        val partitions = othersRepository.getPartitions()
        partitions.forEach {
            val partitionFingerprintProperty = othersRepository.partitionFingerprint(it)
            val fingerprint = try {
                othersRepository.getPartitionFingerprintProperty(partitionFingerprintProperty)
            } catch (e: Exception) {
                val result = "Key `$partitionFingerprintProperty`. Error: ${e.message}. Caused by: ${e.cause?.message}"
                Log.e(javaClass.simpleName, result)
                MyApplication.getMyString(R.string.build_not_filled)
            }
            if (fingerprint != MyApplication.getMyString(R.string.build_not_filled)) {
                add(tempModels, MyApplication.getMyString(R.string.build_certain_fingerprint, it), fingerprint)
            }
        }
    }

    private fun detectBinderStatus(
        tempModels: ArrayList<MyModel>, driver: String, @StringRes titleId: Int
    ) {
        val binderVersion = try {
            othersRepository.getBinderVersionOrThrow(driver)
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        @StringRes val binderStatusId = when (binderVersion) {
            -ArchitectureDataSource.ERRNO_NO_SUCH_FILE_OR_DIRECTORY -> {
                R.string.result_not_supported
            }
            -ArchitectureDataSource.ERRNO_PERMISSION_DENIED -> {
                android.R.string.unknownName
            }
            ArchitectureDataSource.BINDER64_PROTOCOL_VERSION -> {
                R.string.bit_64
            }
            ArchitectureDataSource.BINDER32_PROTOCOL_VERSION -> {
                R.string.bit_32
            }
            else -> {
                android.R.string.unknownName
            }
        }

        add(tempModels, MyApplication.getMyString(titleId), MyApplication.getMyString(binderStatusId))
    }

    private fun getProcessBit(): String {
        val isProcess64Bit = try {
            othersRepository.isProcess64BitOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        return MyApplication.getMyString(
            if (isProcess64Bit) {
                R.string.bit_64
            } else {
                R.string.bit_32
            }
        )
    }
}