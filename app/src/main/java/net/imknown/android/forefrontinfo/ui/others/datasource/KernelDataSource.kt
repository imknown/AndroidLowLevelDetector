package net.imknown.android.forefrontinfo.ui.others.datasource

import net.imknown.android.forefrontinfo.ui.common.getShellResult

class KernelDataSource {
    companion object {
        private const val CMD_KERNEL_VERBOSE = "cat /proc/version"
        private const val CMD_KERNEL_ALL = "uname -a"
    }

    fun getKernelVersion() = getShellResult(CMD_KERNEL_VERBOSE)

    fun getKernelAll() = getShellResult(CMD_KERNEL_ALL)
}