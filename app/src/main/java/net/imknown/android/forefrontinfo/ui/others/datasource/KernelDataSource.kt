package net.imknown.android.forefrontinfo.ui.others.datasource

import net.imknown.android.forefrontinfo.base.shell.getShellResultByCondition

class KernelDataSource {
    companion object {
        private const val CMD_KERNEL_VERBOSE = "cat /proc/version"
        private const val CMD_KERNEL_ALL = "uname -a"
    }

    fun getKernelVersion() = getShellResultByCondition(CMD_KERNEL_VERBOSE)

    fun getKernelAll() = getShellResultByCondition(CMD_KERNEL_ALL)
}