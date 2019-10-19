package net.imknown.android.forefrontinfo.ui.others

import android.os.Build
import androidx.annotation.StringRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override fun collectionDataset(): ArrayList<MyModel> {
        val myDataset = ArrayList<MyModel>()

        //
        myDataset.add(MyModel(getResultString(R.string.build_brand, Build.BRAND)))
        myDataset.add(MyModel(getResultString(R.string.build_manufacturer, Build.MANUFACTURER)))
        myDataset.add(MyModel(getResultString(R.string.build_model, Build.MODEL)))
        myDataset.add(MyModel(getResultString(R.string.build_device, Build.DEVICE)))
        myDataset.add(MyModel(getResultString(R.string.build_product, Build.PRODUCT)))
        myDataset.add(MyModel(getResultString(R.string.build_hardware, Build.HARDWARE)))
        myDataset.add(MyModel(getResultString(R.string.build_board, Build.BOARD)))

        //
        @Suppress("DEPRECATION")
        myDataset.add(MyModel(getResultString(R.string.build_cpu_abi, Build.CPU_ABI)))
        myDataset.add(
            MyModel(
                getResultString(
                    R.string.build_supported_32_bit_abis,
                    Build.SUPPORTED_32_BIT_ABIS.asList().toString()
                )
            )
        )
        myDataset.add(
            MyModel(
                getResultString(
                    R.string.build_supported_64_bit_abis,
                    Build.SUPPORTED_64_BIT_ABIS.asList().toString()
                )
            )
        )

        //
        myDataset.add(
            MyModel(
                getResultString(
                    R.string.build_sdk_int,
                    Build.VERSION.SDK_INT.toString()
                )
            )
        )
        myDataset.add(MyModel(getResultString(R.string.build_release, Build.VERSION.RELEASE)))
        if (isAtLeastAndroid6()) {
            myDataset.add(
                MyModel(
                    getResultString(
                        R.string.build_preview_sdk_int,
                        Build.VERSION.PREVIEW_SDK_INT.toString()
                    )
                )
            )
        }

        //
        myDataset.add(MyModel(getResultString(R.string.build_user, Build.USER)))
        myDataset.add(MyModel(getResultString(R.string.build_HOST, Build.HOST)))
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(Build.TIME))
        myDataset.add(MyModel(getResultString(R.string.build_time, time)))
        if (isAtLeastAndroid6()) {
            myDataset.add(MyModel(getResultString(R.string.build_base_os, Build.VERSION.BASE_OS)))
            myDataset.add(
                MyModel(
                    getResultString(
                        R.string.build_security_patch,
                        Build.VERSION.SECURITY_PATCH
                    )
                )
            )
        }
        myDataset.add(MyModel(getResultString(R.string.build_fingerprint, Build.FINGERPRINT)))
        myDataset.add(MyModel(getResultString(R.string.build_display, Build.DISPLAY)))
        myDataset.add(MyModel(getResultString(R.string.build_id, Build.ID)))
        myDataset.add(
            MyModel(
                getResultString(
                    R.string.build_incremental,
                    Build.VERSION.INCREMENTAL
                )
            )
        )
        myDataset.add(MyModel(getResultString(R.string.build_type, Build.TYPE)))
        myDataset.add(MyModel(getResultString(R.string.build_tags, Build.TAGS)))
        myDataset.add(MyModel(getResultString(R.string.build_codename, Build.VERSION.CODENAME)))

        //
        myDataset.add(MyModel(getResultString(R.string.build_bootloader, Build.BOOTLOADER)))
        myDataset.add(MyModel(getResultString(R.string.build_radio, Build.getRadioVersion())))

        return myDataset
    }

    private fun getResultString(@StringRes stringId: Int, value: String?): String {
        val finalValue = if (value.isNullOrEmpty()) {
            getString(R.string.build_not_filled)
        } else {
            value
        }

        return getString(stringId, finalValue)
    }
}