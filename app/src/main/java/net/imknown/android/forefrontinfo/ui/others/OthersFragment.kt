package net.imknown.android.forefrontinfo.ui.others

import android.os.Build
import androidx.annotation.StringRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment
import java.text.SimpleDateFormat
import java.util.*

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override suspend fun collectionDataset() {
        fillDataset()

        showResult()

        disableSwipeRefresh()
    }

    private fun fillDataset() {
        createNewTempDataset()

        //
        add(getResultString(R.string.build_brand, Build.BRAND))
        add(getResultString(R.string.build_manufacturer, Build.MANUFACTURER))
        add(getResultString(R.string.build_model, Build.MODEL))
        add(getResultString(R.string.build_device, Build.DEVICE))
        add(getResultString(R.string.build_product, Build.PRODUCT))
        add(getResultString(R.string.build_hardware, Build.HARDWARE))
        add(getResultString(R.string.build_board, Build.BOARD))

        //
        @Suppress("DEPRECATION")
        add(getResultString(R.string.build_cpu_abi, Build.CPU_ABI))
        add(
            getResultString(
                R.string.build_supported_32_bit_abis,
                Build.SUPPORTED_32_BIT_ABIS.asList().toString()
            )
        )
        add(
            getResultString(
                R.string.build_supported_64_bit_abis,
                Build.SUPPORTED_64_BIT_ABIS.asList().toString()
            )
        )

        //
        add(getResultString(R.string.build_user, Build.USER))
        add(getResultString(R.string.build_HOST, Build.HOST))
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(Build.TIME))
        add(getResultString(R.string.build_time, time))
        if (isAtLeastAndroid6()) {
            add(getResultString(R.string.build_base_os, Build.VERSION.BASE_OS))
            add(
                getResultString(
                    R.string.build_security_patch,
                    Build.VERSION.SECURITY_PATCH
                )
            )
        }
        add(getResultString(R.string.build_fingerprint, Build.FINGERPRINT))
        add(getResultString(R.string.build_display, Build.DISPLAY))
        add(getResultString(R.string.build_id, Build.ID))
        add(getResultString(R.string.build_incremental, Build.VERSION.INCREMENTAL))
        add(getResultString(R.string.build_type, Build.TYPE))
        add(getResultString(R.string.build_tags, Build.TAGS))
        add(getResultString(R.string.build_codename, Build.VERSION.CODENAME))

        //
        add(getResultString(R.string.build_bootloader, Build.BOOTLOADER))
        add(getResultString(R.string.build_radio, Build.getRadioVersion()))
    }

    private fun getResultString(@StringRes stringId: Int, vararg value: Any?): String {
        return if (
            value.isNullOrEmpty()
            || value[0] == null
            || value[0].toString().isEmpty()
        ) {
            getString(stringId, getString(R.string.build_not_filled))
        } else {
            getString(stringId, *value)
        }
    }
}