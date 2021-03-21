package net.imknown.android.forefrontinfo.ui.others.datasource

import android.os.Build

class OthersDataSource {
    fun getBootloader(): String = Build.BOOTLOADER
    fun getRadioVersionOrNull(): String? = Build.getRadioVersion()
}