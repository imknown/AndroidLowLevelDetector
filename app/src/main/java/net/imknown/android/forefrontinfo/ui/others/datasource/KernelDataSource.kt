package net.imknown.android.forefrontinfo.ui.others.datasource

import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.base.shell.IShell

@Inject
class KernelDataSource(private val shell: IShell) {
    companion object {
        private const val CMD_KERNEL_VERBOSE = "cat /proc/version"
        private const val CMD_KERNEL_ALL = "uname -a"
    }

    fun getKernelVersion() = shell.execute(CMD_KERNEL_VERBOSE)

    fun getKernelAll() = shell.execute(CMD_KERNEL_ALL)
}