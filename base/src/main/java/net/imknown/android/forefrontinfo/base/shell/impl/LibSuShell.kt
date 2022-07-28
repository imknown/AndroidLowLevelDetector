package net.imknown.android.forefrontinfo.base.shell.impl

import com.topjohnwu.superuser.Shell
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

object LibSuShell : IShell {
    override fun execute(cmd: String): ShellResult {
        val result = Shell.cmd(cmd).exec()
        return ShellResult(result.out, result.isSuccess, result.code)
    }
}