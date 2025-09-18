package net.imknown.android.forefrontinfo.ui.others.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.toTranslatedDetailMyModel
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource
import android.R as androidR

class OthersRepository(
    private val basicDataSource: BasicDataSource,
    private val architectureDataSource: ArchitectureDataSource,
    private val romDataSource: RomDataSource,
    private val fingerprintDataSource: FingerprintDataSource,
    private val kernelDataSource: KernelDataSource,
    private val othersDataSource: OthersDataSource
) {
    // region [Basic]
    fun getBrand() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_brand), basicDataSource.getBrand())
    fun getManufacturer() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_manufacturer), basicDataSource.getManufacturer())
    fun getModel() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_model), basicDataSource.getModel())
    fun getDevice() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_device), basicDataSource.getDevice())
    fun getProduct() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_product), basicDataSource.getProduct())
    fun getHardware() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_hardware), basicDataSource.getHardware())
    fun getBoard() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_board), basicDataSource.getBoard())

    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocModel() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_soc_model), basicDataSource.getSocModel())
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocManufacturer() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_soc_manufacturer), basicDataSource.getSocManufacturer())
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSku() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_hardware_sku), basicDataSource.getSku())
    fun getVendorSku() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_vendor_sku),basicDataSource.getVendorSku())
    @RequiresApi(Build.VERSION_CODES.S)
    fun getOdmSku() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_odm_hardware_sku), basicDataSource.getOdmSku())
    // endregion [Basic]

    // region [Binder]
    fun getBinderStatus(driver: String): MyModel {
        val binderVersion = try {
            architectureDataSource.getBinderVersionOrThrow(driver)
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        @StringRes val binderStatusId = when (binderVersion) {
            -ArchitectureDataSource.ERRNO_NO_SUCH_FILE_OR_DIRECTORY -> {
                R.string.result_not_supported
            }
            -ArchitectureDataSource.ERRNO_PERMISSION_DENIED -> {
                androidR.string.unknownName
            }
            ArchitectureDataSource.BINDER64_PROTOCOL_VERSION -> {
                R.string.bit_64
            }
            ArchitectureDataSource.BINDER32_PROTOCOL_VERSION -> {
                R.string.bit_32
            }
            else -> {
                androidR.string.unknownName
            }
        }

        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.binder_status), MyApplication.getMyString(binderStatusId))
    }
    // endregion [Binder]

    // region [Process]
    fun getProcessBit(): MyModel {
        val isProcess64Bit = try {
            architectureDataSource.isProcess64BitOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        val bitId = if (isProcess64Bit) R.string.bit_64 else R.string.bit_32

        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.current_process_bit), MyApplication.getMyString(bitId))
    }

    fun getArchitecture(): MyModel {
        val a = try {
            architectureDataSource.getArchitectureOrNullOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: MyApplication.getMyString(androidR.string.unknownName)
        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.os_arch), a)
    }
    // endregion [Process]

    // region [ABI]
    fun getCpuAbi() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_cpu_abi), architectureDataSource.getCpuAbi())
    fun getPropertyCpuAbi() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.current_system_abi), architectureDataSource.getPropertyCpuAbi())
    fun getSupported32BitAbis() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_supported_32_bit_abis), architectureDataSource.getSupported32BitAbis().joinToString())
    fun getSupported64BitAbis(): MyModel {
        val supported64BitAbis = architectureDataSource.getSupported64BitAbis().joinToString().takeIf { it.isNotEmpty() }
            ?: MyApplication.getMyString(R.string.result_not_supported)
        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_supported_64_bit_abis), supported64BitAbis)
    }
    // endregion [ABI]

    // region [ROM]
    fun getUser() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_user), romDataSource.getUser())
    fun getHost() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_host), romDataSource.getHost())
    fun getTime(): MyModel {
        val time = romDataSource.getTime().formatToLocalZonedDatetimeString()
        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_time), time)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun getBaseOs() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_base_os), romDataSource.getBaseOs())

    fun getId() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_id), romDataSource.getId())
    fun getDisplay() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_display), romDataSource.getDisplay())
    fun getType() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_type), romDataSource.getType())
    fun getTags() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_tags), romDataSource.getTags())
    fun getIncremental() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_incremental), romDataSource.getIncremental())
    fun getCodename() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_codename), romDataSource.getCodename())
    @RequiresApi(Build.VERSION_CODES.M)
    fun getPreviewSdkInt() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_preview_sdk_int), romDataSource.getPreviewSdkInt().toString())

    // region [Fingerprint]
    fun getFingerprint() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_stock_fingerprint), fingerprintDataSource.getFingerprint())
    fun getPreviewSdkFingerprint() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_stock_preview_fingerprint), fingerprintDataSource.getPreviewSdkFingerprint())
    fun getPartitionFingerprints(): List<MyModel> {
        val partitions = fingerprintDataSource.getPartitions()
        return partitions.mapNotNull {
            val partitionFingerprintProperty = fingerprintDataSource.getPartitionFingerprint(it)
            val fingerprint = fingerprintDataSource.getPartitionFingerprintProperty(partitionFingerprintProperty)
            if (fingerprint != MyApplication.getMyString(R.string.build_not_filled)
                && fingerprint != MyApplication.getMyString(R.string.result_not_supported)
            ) {
                val title = MyApplication.getMyString(R.string.build_certain_fingerprint, it)
                toTranslatedDetailMyModel(title, fingerprint)
            } else {
                null
            }
        }
    }
    // endregion [Fingerprint]

    fun getDefaultUserAgent(context: Context): MyModel {
        val userAgent = try {
            romDataSource.getDefaultUserAgentOrThrow(context)
        } catch (e: Exception) {
            e.printStackTrace()
            MyApplication.getMyString(androidR.string.unknownName)
        }
        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.webview_user_agent), userAgent)
    }

    fun getKernelVersion(): MyModel {
        var kernelFinal: String? = null
        val kernelVerbose = kernelDataSource.getKernelVersion()
        if (kernelVerbose.isSuccess) {
            kernelFinal = kernelVerbose.output.getOrNull(0)
        } else {
            val kernelAll = kernelDataSource.getKernelAll()
            if (kernelAll.isSuccess) {
                kernelFinal = kernelAll.output.getOrNull(0)
            }
        }

        return toTranslatedDetailMyModel(MyApplication.getMyString(R.string.linux), kernelFinal)
    }
    // endregion [ROM]

    // region [Others]
    fun getBootloader() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_bootloader), othersDataSource.getBootloader())
    fun getRadioVersionOrNull() = toTranslatedDetailMyModel(MyApplication.getMyString(R.string.build_radio), othersDataSource.getRadioVersionOrNull())
    // endregion [Others]
}