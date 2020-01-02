package net.imknown.android.forefrontinfo.ui.others

import android.content.SharedPreferences
import android.os.Build
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment
import net.imknown.android.forefrontinfo.base.GetRawPropEventViewModel
import java.text.SimpleDateFormat
import java.util.*

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    private val getRawPropEventViewModel: GetRawPropEventViewModel by activityViewModels()

    override suspend fun collectionDataset() {
        fillDataset()

        if (isActivityAndFragmentOk(this)) {
            showResult()

            // disableSwipeRefresh()
        }
    }

    private suspend fun fillDataset() {
        createNewTempDataset()

        // region [Basic]
        add(MyApplication.getMyString(R.string.build_brand), Build.BRAND)
        add(MyApplication.getMyString(R.string.build_manufacturer), Build.MANUFACTURER)
        add(MyApplication.getMyString(R.string.build_model), Build.MODEL)
        add(MyApplication.getMyString(R.string.build_device), Build.DEVICE)
        add(MyApplication.getMyString(R.string.build_product), Build.PRODUCT)
        add(MyApplication.getMyString(R.string.build_hardware), Build.HARDWARE)
        add(MyApplication.getMyString(R.string.build_board), Build.BOARD)
        // endregion [Basic]

        // region [Arch & ABI]
        add(MyApplication.getMyString(R.string.os_arch), System.getProperty("os.arch").toString())
        @Suppress("DEPRECATION")
        add(MyApplication.getMyString(R.string.build_cpu_abi), Build.CPU_ABI)
        add(
            MyApplication.getMyString(R.string.build_supported_32_bit_abis),
            Build.SUPPORTED_32_BIT_ABIS.joinToString()
        )
        add(
            MyApplication.getMyString(R.string.build_supported_64_bit_abis),
            Build.SUPPORTED_64_BIT_ABIS.joinToString().takeIf { it.isNotEmpty() }
                ?: MyApplication.getMyString(R.string.result_not_supported)
        )
        // endregion [Arch & ABI]

        // endregion [ROM]
        add(MyApplication.getMyString(R.string.build_user), Build.USER)
        add(MyApplication.getMyString(R.string.build_HOST), Build.HOST)
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(Build.TIME))
        add(MyApplication.getMyString(R.string.build_time), time)
        if (isAtLeastAndroid6()) {
            add(MyApplication.getMyString(R.string.build_base_os), Build.VERSION.BASE_OS)
        }
        add(MyApplication.getMyString(R.string.build_fingerprint), Build.FINGERPRINT)
        add(MyApplication.getMyString(R.string.build_display), Build.DISPLAY)
        add(MyApplication.getMyString(R.string.build_incremental), Build.VERSION.INCREMENTAL)
        add(MyApplication.getMyString(R.string.build_type), Build.TYPE)
        add(MyApplication.getMyString(R.string.build_tags), Build.TAGS)
        add(MyApplication.getMyString(R.string.build_codename), Build.VERSION.CODENAME)
        // endregion [ROM]

        // endregion [Others]
        add(MyApplication.getMyString(R.string.build_bootloader), Build.BOOTLOADER)
        add(MyApplication.getMyString(R.string.build_radio), Build.getRadioVersion())
        // endregion [Others]

        getProp()
    }

    private suspend fun getProp() {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
        val rawBuildProp = sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_raw_build_prop_key), false
        )

        if (!rawBuildProp) {
            withContext(Dispatchers.Main) {
                getRawPropEventViewModel.onFinish()
            }

            return
        }

        var temp = ""
        sh("getprop").forEach {
            if (it.startsWith("[") && it.endsWith("]")) {
                addRawProp(it)
            } else {
                temp += "$it\n"

                if (it.endsWith("]")) {
                    addRawProp(temp)

                    temp = ""
                }
            }
        }

        withContext(Dispatchers.Main) {
            getRawPropEventViewModel.onFinish()
        }
    }

    private fun addRawProp(text: String) {
        val result = text.split(": ")
        add(removeSquareBrackets(result[0]), removeSquareBrackets(result[1]))
    }

    private fun removeSquareBrackets(text: String) =
        text.substringAfter("[").substringBefore(']').trimIndent()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        super.onSharedPreferenceChanged(sharedPreferences, key)

        launch(Dispatchers.IO) {
            if (key == MyApplication.getMyString(R.string.function_raw_build_prop_key)) {
                collectionDatasetCaller(500)
            }
        }
    }
}