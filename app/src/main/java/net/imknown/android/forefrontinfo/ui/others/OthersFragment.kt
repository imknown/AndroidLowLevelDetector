package net.imknown.android.forefrontinfo.ui.others

import android.os.Build
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment
import java.text.SimpleDateFormat
import java.util.*

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override fun collectionDataset() {
        //
        add(MyModel(getResultString(R.string.build_brand, Build.BRAND)))
        add(MyModel(getResultString(R.string.build_manufacturer, Build.MANUFACTURER)))
        add(MyModel(getResultString(R.string.build_model, Build.MODEL)))
        add(MyModel(getResultString(R.string.build_device, Build.DEVICE)))
        add(MyModel(getResultString(R.string.build_product, Build.PRODUCT)))
        add(MyModel(getResultString(R.string.build_hardware, Build.HARDWARE)))
        add(MyModel(getResultString(R.string.build_board, Build.BOARD)))

        //
        @Suppress("DEPRECATION")
        add(MyModel(getResultString(R.string.build_cpu_abi, Build.CPU_ABI)))
        add(
            MyModel(
                getResultString(
                    R.string.build_supported_32_bit_abis,
                    Build.SUPPORTED_32_BIT_ABIS.asList().toString()
                )
            )
        )
        add(
            MyModel(
                getResultString(
                    R.string.build_supported_64_bit_abis,
                    Build.SUPPORTED_64_BIT_ABIS.asList().toString()
                )
            )
        )

        //
        add(MyModel(getResultString(R.string.build_user, Build.USER)))
        add(MyModel(getResultString(R.string.build_HOST, Build.HOST)))
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(Build.TIME))
        add(MyModel(getResultString(R.string.build_time, time)))
        if (isAtLeastAndroid6()) {
            add(MyModel(getResultString(R.string.build_base_os, Build.VERSION.BASE_OS)))
            add(
                MyModel(
                    getResultString(
                        R.string.build_security_patch,
                        Build.VERSION.SECURITY_PATCH
                    )
                )
            )
        }
        add(MyModel(getResultString(R.string.build_fingerprint, Build.FINGERPRINT)))
        add(MyModel(getResultString(R.string.build_display, Build.DISPLAY)))
        add(MyModel(getResultString(R.string.build_id, Build.ID)))
        add(
            MyModel(
                getResultString(
                    R.string.build_incremental,
                    Build.VERSION.INCREMENTAL
                )
            )
        )
        add(MyModel(getResultString(R.string.build_type, Build.TYPE)))
        add(MyModel(getResultString(R.string.build_tags, Build.TAGS)))
        add(MyModel(getResultString(R.string.build_codename, Build.VERSION.CODENAME)))

        //
        add(MyModel(getResultString(R.string.build_bootloader, Build.BOOTLOADER)))
        add(MyModel(getResultString(R.string.build_radio, Build.getRadioVersion())))
    }
}