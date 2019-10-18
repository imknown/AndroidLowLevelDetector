package net.imknown.android.forefrontinfo.ui.others

import android.os.Build
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

        myDataset.add(MyModel("品牌\n${Build.BRAND}"))
        myDataset.add(MyModel("代工\n${Build.MANUFACTURER}"))
        myDataset.add(MyModel("销售型号\n${Build.MODEL}"))
        myDataset.add(MyModel("开发代号\n${Build.DEVICE}"))
        myDataset.add(MyModel("具体型号\n${Build.PRODUCT}"))
        myDataset.add(MyModel("SoC/内核源码 提供商\n${Build.HARDWARE}"))
        myDataset.add(MyModel("SoC 型号\n${Build.BOARD}"))


        @Suppress("DEPRECATION")
        myDataset.add(MyModel("系统正在使用的架构\n${Build.CPU_ABI}"))
        myDataset.add(MyModel("所有受支持的32位架构\n${Build.SUPPORTED_32_BIT_ABIS.asList()}"))
        myDataset.add(MyModel("所有受支持的64位架构\n${Build.SUPPORTED_64_BIT_ABIS.asList()}"))


        myDataset.add(MyModel("当前系统 API 等级\n${Build.VERSION.SDK_INT}"))
        myDataset.add(MyModel("当前系统 版本号\n${Build.VERSION.RELEASE}"))
        myDataset.add(MyModel("当前系统 版本\n${Build.DISPLAY}"))
        if (isAtLeastAndroid6()) {
            myDataset.add(MyModel("当前系统 预览的 API 等级 (0 表示 正式)\n${Build.VERSION.PREVIEW_SDK_INT}"))
        }


        myDataset.add(MyModel("当前系统 构建者\n${Build.USER}"))
        val time =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(Build.TIME))
        myDataset.add(MyModel("当前系统 构建时间\n$time"))
        if (isAtLeastAndroid6()) {
            myDataset.add(MyModel("基于什么系统 二次开发\n${Build.VERSION.BASE_OS}"))
            myDataset.add(MyModel("当前系统 安全补丁版本\n${Build.VERSION.SECURITY_PATCH}"))
        }
        myDataset.add(MyModel("官方系统 指纹\n${Build.FINGERPRINT}"))
        myDataset.add(MyModel("当前系统 Git 修订号\n${Build.ID}"))
        myDataset.add(MyModel("当前系统 Git 提交 Hash\n${Build.VERSION.INCREMENTAL}"))
        myDataset.add(MyModel("当前系统 构建类型\n${Build.TYPE}"))
        myDataset.add(MyModel("当前系统 构建描述标签\n${Build.TAGS}"))
        myDataset.add(MyModel("当前系统 构建分支名 (REL 表示 正式)\n${Build.VERSION.CODENAME}"))
        myDataset.add(MyModel("当前系统 构建的主机名\n${Build.HOST}"))


        myDataset.add(MyModel("Bootloader 版本号\n${Build.BOOTLOADER}"))
        myDataset.add(MyModel("基带 版本号\n${Build.getRadioVersion()}"))

        return myDataset
    }
}