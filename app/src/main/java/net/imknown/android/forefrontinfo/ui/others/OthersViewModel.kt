package net.imknown.android.forefrontinfo.ui.others

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.ui.base.BasePureListViewModel
import net.imknown.android.forefrontinfo.ui.base.IAndroidVersion
import net.imknown.android.forefrontinfo.ui.base.MyModel
import java.util.*

class OthersViewModel : BasePureListViewModel(), IAndroidVersion {

    companion object {
        private const val PROP_RO_PRODUCT_CPU_ABI = "ro.product.cpu.abi"
        private const val SYSTEM_PROPERTY_ARCHITECTURE = "os.arch"

        // private const val CPU_ARCHITECTURE = "grep 'CPU architecture' /proc/cpuinfo"
        private const val DRIVER_BINDER = "/dev/binder"

        private const val ERRNO_NO_SUCH_FILE_OR_DIRECTORY = 2
        private const val ERRNO_PERMISSION_DENIED = 13
        private const val BINDER32_PROTOCOL_VERSION = 7
        private const val BINDER64_PROTOCOL_VERSION = 8

        private const val PROP_PREVIEW_SDK_FINGERPRINT = "ro.build.version.preview_sdk_fingerprint"
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getProcessBit(): String {
        val isProcess64Bit = if (isAtLeastStableAndroid6()) {
            android.os.Process.is64Bit()
        } else {
            val vmRuntimePath = "dalvik.system.VMRuntime"
            val vmRuntimeInstance = Class.forName(vmRuntimePath)
                .getDeclaredMethod("getRuntime")
                .invoke(null)

            Class.forName(vmRuntimePath)
                .getDeclaredMethod("is64Bit")
                .invoke(vmRuntimeInstance) as Boolean
        }

        return MyApplication.getMyString(
            if (isProcess64Bit) {
                R.string.bit_64
            } else {
                R.string.bit_32
            }
        )
    }

    @ExperimentalStdlibApi
    override fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
        val tempModels = ArrayList<MyModel>()

        // region [Basic]
        add(tempModels, MyApplication.getMyString(R.string.build_brand), Build.BRAND)
        add(tempModels, MyApplication.getMyString(R.string.build_manufacturer), Build.MANUFACTURER)
        add(tempModels, MyApplication.getMyString(R.string.build_model), Build.MODEL)
        add(tempModels, MyApplication.getMyString(R.string.build_device), Build.DEVICE)
        add(tempModels, MyApplication.getMyString(R.string.build_product), Build.PRODUCT)
        add(tempModels, MyApplication.getMyString(R.string.build_hardware), Build.HARDWARE)
        add(tempModels, MyApplication.getMyString(R.string.build_board), Build.BOARD)
        // endregion [Basic]

        // region [Arch & ABI]
        // region [Binder]
        detectBinderStatus(tempModels, DRIVER_BINDER, R.string.binder_status)
        // endregion [Binder]

        // region [Process]
        add(tempModels, MyApplication.getMyString(R.string.current_process_bit), getProcessBit())
        add(
            tempModels,
            MyApplication.getMyString(R.string.os_arch),
            System.getProperty(SYSTEM_PROPERTY_ARCHITECTURE)
        )
        @Suppress("DEPRECATION")
        add(tempModels, MyApplication.getMyString(R.string.build_cpu_abi), Build.CPU_ABI)
        // endregion [Process]

        add(
            tempModels,
            MyApplication.getMyString(R.string.current_system_abi),
            getStringProperty(PROP_RO_PRODUCT_CPU_ABI)
        )
        add(
            tempModels,
            MyApplication.getMyString(R.string.build_supported_32_bit_abis),
            Build.SUPPORTED_32_BIT_ABIS.joinToString()
        )
        add(tempModels,
            MyApplication.getMyString(R.string.build_supported_64_bit_abis),
            Build.SUPPORTED_64_BIT_ABIS.joinToString().takeIf { it.isNotEmpty() }
                ?: MyApplication.getMyString(R.string.result_not_supported)
        )
        // endregion [Arch & ABI]

        // region [ROM]
        add(tempModels, MyApplication.getMyString(R.string.build_user), Build.USER)
        add(tempModels, MyApplication.getMyString(R.string.build_HOST), Build.HOST)
        val time = Build.TIME.formatToLocalZonedDatetimeString()
        add(tempModels, MyApplication.getMyString(R.string.build_time), time)
        if (isAtLeastStableAndroid6()) {
            add(
                tempModels,
                MyApplication.getMyString(R.string.build_base_os),
                Build.VERSION.BASE_OS
            )
        }
        addFingerprints(tempModels)
        add(tempModels, MyApplication.getMyString(R.string.build_display), Build.DISPLAY)
        add(
            tempModels,
            MyApplication.getMyString(R.string.build_incremental),
            Build.VERSION.INCREMENTAL
        )
        add(tempModels, MyApplication.getMyString(R.string.build_type), Build.TYPE)
        add(tempModels, MyApplication.getMyString(R.string.build_tags), Build.TAGS)
        add(tempModels, MyApplication.getMyString(R.string.build_codename), Build.VERSION.CODENAME)
        // endregion [ROM]

        // region [Others]
        add(tempModels, MyApplication.getMyString(R.string.build_bootloader), Build.BOOTLOADER)
        add(tempModels, MyApplication.getMyString(R.string.build_radio), Build.getRadioVersion())
        // endregion [Others]

        setModels(tempModels)
    }

    @ExperimentalStdlibApi
    private fun addFingerprints(tempModels: ArrayList<MyModel>) {
        if (isAtLeastStableAndroid10()) {
            Build.getFingerprintedPartitions().forEach {
                add(
                    tempModels,
                    MyApplication.getMyString(
                        R.string.build_certain_fingerprint,
                        it.name.capitalize(Locale.US)
                    ),
                    it.fingerprint
                )
            }
        } else {
            add(
                tempModels,
                MyApplication.getMyString(R.string.build_stock_fingerprint),
                Build.FINGERPRINT
            )
        }

        if (isAtLeastStableAndroid10()) {
            add(
                tempModels,
                MyApplication.getMyString(R.string.build_stock_preview_fingerprint),
                // Build.VERSION.PREVIEW_SDK_FINGERPRINT
                getStringProperty(PROP_PREVIEW_SDK_FINGERPRINT)
            )
        }
    }

    private external fun getBinderVersion(driver: String): Int

    private fun detectBinderStatus(
        tempModels: ArrayList<MyModel>,
        driver: String,
        @StringRes titleId: Int
    ) {
        val binderVersion = try {
            System.loadLibrary("BinderDetector")
            getBinderVersion(driver)
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        @StringRes val binderStatusId = when (binderVersion) {
            -ERRNO_NO_SUCH_FILE_OR_DIRECTORY -> {
                R.string.result_not_supported
            }
            -ERRNO_PERMISSION_DENIED -> {
                android.R.string.unknownName
            }
            BINDER64_PROTOCOL_VERSION -> {
                R.string.bit_64
            }
            BINDER32_PROTOCOL_VERSION -> {
                R.string.bit_32
            }
            else -> {
                android.R.string.unknownName
            }
        }

        add(
            tempModels,
            MyApplication.getMyString(titleId),
            MyApplication.getMyString(binderStatusId)
        )
    }
}