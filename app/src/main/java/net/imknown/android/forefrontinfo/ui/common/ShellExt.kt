package net.imknown.android.forefrontinfo.ui.common

import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.ShellResult

fun getShellResult(cmd: String, condition: Boolean = true) = if (condition) {
    ShellManager.instance.execute(cmd)
} else {
    ShellResult()
}