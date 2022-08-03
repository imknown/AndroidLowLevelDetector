package net.imknown.android.forefrontinfo.base.shell

object ShellManager {
    lateinit var shell: IShell

    fun execute(cmd: String) = shell.execute(cmd)
}