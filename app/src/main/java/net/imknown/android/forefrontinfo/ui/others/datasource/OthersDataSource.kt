package net.imknown.android.forefrontinfo.ui.others.datasource

import android.os.Build
import dev.zacsweers.metro.Inject

@Inject
class OthersDataSource {
    fun getBootloader(): String = Build.BOOTLOADER
    fun getRadioVersionOrNull(): String? = Build.getRadioVersion()
}