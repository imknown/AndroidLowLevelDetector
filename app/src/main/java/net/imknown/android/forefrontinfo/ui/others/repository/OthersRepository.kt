package net.imknown.android.forefrontinfo.ui.others.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.guardedDetectFailedMyModel
import net.imknown.android.forefrontinfo.ui.base.list.toTranslatedDetailMyModel
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid13
import net.imknown.android.forefrontinfo.ui.common.isPreviewAndroid
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource
import android.R as androidR

// Constructor injection puts the dependency chain on the signature and lets Metro build it
// (issues-cn #02); every param is a concrete @Inject type, so no @Provides is needed.
@Inject
class OthersRepository(
    private val basicDataSource: BasicDataSource,
    private val architectureDataSource: ArchitectureDataSource,
    private val romDataSource: RomDataSource,
    private val fingerprintDataSource: FingerprintDataSource,
    private val kernelDataSource: KernelDataSource,
    private val othersDataSource: OthersDataSource
) {
    // region [Basic]
    fun getBrand() = guardedDetectFailedMyModel(R.string.build_brand) {
        toTranslatedDetailMyModel(R.string.build_brand, basicDataSource.getBrand())
    }
    fun getManufacturer() = guardedDetectFailedMyModel(R.string.build_manufacturer) {
        toTranslatedDetailMyModel(R.string.build_manufacturer, basicDataSource.getManufacturer())
    }
    fun getModel() = guardedDetectFailedMyModel(R.string.build_model) {
        toTranslatedDetailMyModel(R.string.build_model, basicDataSource.getModel())
    }
    fun getDevice() = guardedDetectFailedMyModel(R.string.build_device) {
        toTranslatedDetailMyModel(R.string.build_device, basicDataSource.getDevice())
    }
    fun getProduct() = guardedDetectFailedMyModel(R.string.build_product) {
        toTranslatedDetailMyModel(R.string.build_product, basicDataSource.getProduct())
    }
    fun getHardware() = guardedDetectFailedMyModel(R.string.build_hardware) {
        toTranslatedDetailMyModel(R.string.build_hardware, basicDataSource.getHardware())
    }
    fun getBoard() = guardedDetectFailedMyModel(R.string.build_board) {
        toTranslatedDetailMyModel(R.string.build_board, basicDataSource.getBoard())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocModel() = guardedDetectFailedMyModel(R.string.build_soc_model) {
        toTranslatedDetailMyModel(R.string.build_soc_model, basicDataSource.getSocModel())
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocManufacturer() = guardedDetectFailedMyModel(R.string.build_soc_manufacturer) {
        toTranslatedDetailMyModel(R.string.build_soc_manufacturer, basicDataSource.getSocManufacturer())
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSku() = guardedDetectFailedMyModel(R.string.build_hardware_sku) {
        toTranslatedDetailMyModel(R.string.build_hardware_sku, basicDataSource.getSku())
    }
    fun getVendorSku() = guardedDetectFailedMyModel(R.string.build_vendor_sku) {
        toTranslatedDetailMyModel(R.string.build_vendor_sku, basicDataSource.getVendorSku())
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun getOdmSku() = guardedDetectFailedMyModel(R.string.build_odm_hardware_sku) {
        toTranslatedDetailMyModel(R.string.build_odm_hardware_sku, basicDataSource.getOdmSku())
    }
    // endregion [Basic]

    // region [Binder]
    fun getBinderStatus(driver: String): MyModel =
        guardedDetectFailedMyModel(R.string.binder_status) {
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

            return toTranslatedDetailMyModel(R.string.binder_status, MyApplication.getMyString(binderStatusId))
        }
    // endregion [Binder]

    // region [Process]
    fun getProcessBit(): MyModel =
        guardedDetectFailedMyModel(R.string.current_process_bit) {
            val isProcess64Bit = try {
                architectureDataSource.isProcess64BitOrThrow()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            val bitId = if (isProcess64Bit) R.string.bit_64 else R.string.bit_32

            return toTranslatedDetailMyModel(R.string.current_process_bit, MyApplication.getMyString(bitId))
        }

    fun getArchitecture(): MyModel =
        guardedDetectFailedMyModel(R.string.os_arch) {
            val a = try {
                architectureDataSource.getArchitectureOrNullOrThrow()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: MyApplication.getMyString(androidR.string.unknownName)
            return toTranslatedDetailMyModel(R.string.os_arch, a)
        }
    // endregion [Process]

    // region [ABI]
    fun getCpuAbi() = guardedDetectFailedMyModel(R.string.build_cpu_abi) {
        toTranslatedDetailMyModel(R.string.build_cpu_abi, architectureDataSource.getCpuAbi())
    }
    fun getPropertyCpuAbi() = guardedDetectFailedMyModel(R.string.current_system_abi) {
        toTranslatedDetailMyModel(R.string.current_system_abi, architectureDataSource.getPropertyCpuAbi())
    }
    fun getSupported32BitAbis() = guardedDetectFailedMyModel(R.string.build_supported_32_bit_abis) {
        toTranslatedDetailMyModel(R.string.build_supported_32_bit_abis, architectureDataSource.getSupported32BitAbis().joinToString())
    }
    fun getSupported64BitAbis(): MyModel =
        guardedDetectFailedMyModel(R.string.build_supported_64_bit_abis) {
            val supported64BitAbis = architectureDataSource.getSupported64BitAbis().joinToString().takeIf { it.isNotEmpty() }
                ?: MyApplication.getMyString(R.string.result_not_supported)
            return toTranslatedDetailMyModel(R.string.build_supported_64_bit_abis, supported64BitAbis)
        }
    // endregion [ABI]

    // region [ROM]
    fun getUser() = guardedDetectFailedMyModel(R.string.build_user) {
        toTranslatedDetailMyModel(R.string.build_user, romDataSource.getUser())
    }
    fun getHost() = guardedDetectFailedMyModel(R.string.build_host) {
        toTranslatedDetailMyModel(R.string.build_host, romDataSource.getHost())
    }
    fun getTime(): MyModel =
        guardedDetectFailedMyModel(R.string.build_time) {
            val time = romDataSource.getTime().formatToLocalZonedDatetimeString()
            return toTranslatedDetailMyModel(R.string.build_time, time)
        }
    fun getBaseOs() = guardedDetectFailedMyModel(R.string.build_base_os) {
        toTranslatedDetailMyModel(R.string.build_base_os, romDataSource.getBaseOs())
    }

    fun getId() = guardedDetectFailedMyModel(R.string.build_id) {
        toTranslatedDetailMyModel(R.string.build_id, romDataSource.getId())
    }
    fun getDisplay() = guardedDetectFailedMyModel(R.string.build_display) {
        toTranslatedDetailMyModel(R.string.build_display, romDataSource.getDisplay())
    }
    fun getType() = guardedDetectFailedMyModel(R.string.build_type) {
        toTranslatedDetailMyModel(R.string.build_type, romDataSource.getType())
    }
    fun getTags() = guardedDetectFailedMyModel(R.string.build_tags) {
        toTranslatedDetailMyModel(R.string.build_tags, romDataSource.getTags())
    }
    fun getIncremental() = guardedDetectFailedMyModel(R.string.build_incremental) {
        toTranslatedDetailMyModel(R.string.build_incremental, romDataSource.getIncremental())
    }
    fun getCodename(): MyModel =
        guardedDetectFailedMyModel(R.string.build_codename) {
            var detail = romDataSource.getCodename()
            if (isPreviewAndroid() && isAtLeastAndroid13()) {
                val previewDisplay = romDataSource.getReleaseOrPreviewDisplay()
                if (detail != previewDisplay) {
                    detail += " ($previewDisplay)"
                }
            }
            return toTranslatedDetailMyModel(R.string.build_codename, detail)
        }
    fun getPreviewSdkInt() = guardedDetectFailedMyModel(R.string.build_preview_sdk_int) {
        toTranslatedDetailMyModel(R.string.build_preview_sdk_int, romDataSource.getPreviewSdkInt().toString())
    }

    // region [Fingerprint]
    fun getFingerprint() = guardedDetectFailedMyModel(R.string.build_stock_fingerprint) {
        toTranslatedDetailMyModel(R.string.build_stock_fingerprint, fingerprintDataSource.getFingerprint())
    }
    fun getPreviewSdkFingerprint() = guardedDetectFailedMyModel(R.string.build_stock_preview_fingerprint) {
        toTranslatedDetailMyModel(R.string.build_stock_preview_fingerprint, fingerprintDataSource.getPreviewSdkFingerprint())
    }
    fun getPartitionFingerprints(): List<MyModel> {
        val partitions = fingerprintDataSource.getPartitions()
        // Per-partition isolation: the partition name is the row title, so a failing read
        // degrades into that partition's own error row and the others still render.
        return partitions.mapNotNull { partition ->
            try {
                val partitionFingerprintProperty = fingerprintDataSource.getPartitionFingerprint(partition)
                val fingerprint = fingerprintDataSource.getPartitionFingerprintProperty(partitionFingerprintProperty)
                if (fingerprint != MyApplication.getMyString(R.string.build_not_filled)
                    && fingerprint != MyApplication.getMyString(R.string.result_not_supported)
                ) {
                    val title = MyApplication.getMyString(R.string.build_certain_fingerprint, partition)
                    toTranslatedDetailMyModel(title, fingerprint)
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                toTranslatedDetailMyModel(
                    partition,
                    MyApplication.getMyString(R.string.result_detect_failed, e.fullMessage)
                )
            }
        }
    }
    // endregion [Fingerprint]

    fun getDefaultUserAgent(context: Context): MyModel =
        guardedDetectFailedMyModel(R.string.webview_user_agent) {
            val userAgent = try {
                romDataSource.getDefaultUserAgentOrThrow(context)
            } catch (e: Exception) {
                e.printStackTrace()
                MyApplication.getMyString(androidR.string.unknownName)
            }
            return toTranslatedDetailMyModel(R.string.webview_user_agent, userAgent)
        }

    fun getKernelVersion(): MyModel =
        guardedDetectFailedMyModel(R.string.linux) {
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

            return toTranslatedDetailMyModel(R.string.linux, kernelFinal)
        }
    // endregion [ROM]

    // region [Others]
    fun getBootloader() = guardedDetectFailedMyModel(R.string.build_bootloader) {
        toTranslatedDetailMyModel(R.string.build_bootloader, othersDataSource.getBootloader())
    }
    fun getRadioVersionOrNull() = guardedDetectFailedMyModel(R.string.build_radio) {
        toTranslatedDetailMyModel(R.string.build_radio, othersDataSource.getRadioVersionOrNull())
    }
    // endregion [Others]
}