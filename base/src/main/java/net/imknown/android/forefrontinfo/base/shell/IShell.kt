package net.imknown.android.forefrontinfo.base.shell

interface IShell {
    fun execute(cmd: String): ShellResult
}