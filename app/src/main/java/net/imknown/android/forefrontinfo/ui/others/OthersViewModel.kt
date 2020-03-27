package net.imknown.android.forefrontinfo.ui.others

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event
import net.imknown.android.forefrontinfo.base.booleanEventLiveData
import net.imknown.android.forefrontinfo.ui.base.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.MyModel
import java.text.SimpleDateFormat
import java.util.*

class OthersViewModel : BaseListViewModel() {

    companion object {
        private const val CMD_GETPROP = "getprop"

        private const val PROP_RO_PRODUCT_CPU_ABI = "ro.product.cpu.abi"

        private const val DRIVER_BINDER = "/dev/binder"
        private const val DRIVER_HW_BINDER = "/dev/hwbinder"
        private const val DRIVER_VND_BINDER = "/dev/vndbinder"

        private const val ERRNO_NO_SUCH_FILE_OR_DIRECTORY = 2
        private const val ERRNO_PERMISSION_DENIED = 13
        private const val BINDER32_PROTOCOL_VERSION = 7
        private const val BINDER64_PROTOCOL_VERSION = 8

        private const val PROP_PREVIEW_SDK_FINGERPRINT = "ro.build.version.preview_sdk_fingerprint"
    }

    private val _rawProp by lazy {
        MyApplication.sharedPreferences.booleanEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.function_raw_build_prop_key),
            false
        )
    }
    val rawProp: LiveData<Event<Boolean>> by lazy { _rawProp }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getProcessBit(): String {
        val isProcess64Bit = if (isAtLeastAndroid6()) {
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
        add(tempModels, MyApplication.getMyString(R.string.current_process_bit), getProcessBit())
        // region [Binder]
        detectBinderStatus(tempModels, DRIVER_BINDER, R.string.binder_status)
        detectBinderStatus(tempModels, DRIVER_HW_BINDER, R.string.hw_binder_status)
        detectBinderStatus(tempModels, DRIVER_VND_BINDER, R.string.vnd_binder_status)
        // endregion [Binder]
        add(tempModels, MyApplication.getMyString(R.string.os_arch), System.getProperty("os.arch"))
        add(
            tempModels,
            MyApplication.getMyString(R.string.current_system_abi),
            getStringProperty(PROP_RO_PRODUCT_CPU_ABI)
        )
        @Suppress("DEPRECATION")
        add(tempModels, MyApplication.getMyString(R.string.build_cpu_abi), Build.CPU_ABI)
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
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(Build.TIME))
        add(tempModels, MyApplication.getMyString(R.string.build_time), time)
        if (isAtLeastAndroid6()) {
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

        getProp(tempModels)

        withContext(Dispatchers.Main) {
            _models.value = tempModels
        }
    }

    @ExperimentalStdlibApi
    private fun addFingerprints(tempModels: ArrayList<MyModel>) {
        if (isAtLeastAndroid10()) {
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

        if (isAtLeastAndroid10()) {
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
        System.loadLibrary("BinderDetector")

        val binderVersion = getBinderVersion(driver)

        @StringRes val binderStatusId = if (binderVersion == -ERRNO_NO_SUCH_FILE_OR_DIRECTORY) {
            R.string.result_not_supported
        } else if (binderVersion == -ERRNO_PERMISSION_DENIED) {
            android.R.string.unknownName
        } else if (binderVersion == BINDER64_PROTOCOL_VERSION) {
            if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
                R.string.abi64_binder64
            } else {
                R.string.abi32_binder64
            }
        } else if (binderVersion == BINDER32_PROTOCOL_VERSION) {
            R.string.abi32_binder32
        } else {
            android.R.string.unknownName
        }

        add(
            tempModels,
            MyApplication.getMyString(titleId),
            MyApplication.getMyString(binderStatusId)
        )
    }

    private fun getProp(tempModels: ArrayList<MyModel>) {
        val rawBuildProp = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_raw_build_prop_key), false
        )

        if (!rawBuildProp) {
            return
        }

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

    private fun add(tempModels: ArrayList<MyModel>, title: String, detail: String?) {
        val translatedDetail = if (detail.isNullOrEmpty()) {
            MyApplication.getMyString(R.string.build_not_filled)
        } else {
            detail.toString()
        }

        tempModels.add(MyModel(title, translatedDetail))
    }
}