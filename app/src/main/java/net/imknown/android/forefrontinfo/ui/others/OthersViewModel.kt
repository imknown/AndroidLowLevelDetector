package net.imknown.android.forefrontinfo.ui.others

import android.os.Build
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListViewModel
import net.imknown.android.forefrontinfo.base.MyModel
import net.imknown.android.forefrontinfo.ui.settings.booleanEventLiveData
import java.text.SimpleDateFormat
import java.util.*

class OthersViewModel : BaseListViewModel() {

    companion object {
        private const val CMD_GETPROP = "getprop"
    }

    val rawProp by lazy {
        MyApplication.sharedPreferences.booleanEventLiveData(
            MyApplication.getMyString(R.string.function_raw_build_prop_key),
            false
        )
    }

    override suspend fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
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
        add(tempModels, MyApplication.getMyString(R.string.os_arch), System.getProperty("os.arch"))
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

        // endregion [ROM]
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
        add(tempModels, MyApplication.getMyString(R.string.build_fingerprint), Build.FINGERPRINT)
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

        // endregion [Others]
        add(tempModels, MyApplication.getMyString(R.string.build_bootloader), Build.BOOTLOADER)
        add(tempModels, MyApplication.getMyString(R.string.build_radio), Build.getRadioVersion())
        // endregion [Others]

        getProp(tempModels)

        withContext(Dispatchers.Main) {
            models.value = tempModels
        }
    }

    private suspend fun getProp(tempModels: ArrayList<MyModel>) {
        val rawBuildProp = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_raw_build_prop_key), false
        )

        if (!rawBuildProp) {
            return
        }

        var temp = ""
        sh(CMD_GETPROP).forEach {
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