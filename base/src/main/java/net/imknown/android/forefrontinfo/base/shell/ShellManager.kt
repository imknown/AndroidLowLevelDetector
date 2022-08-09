package net.imknown.android.forefrontinfo.base.shell

class ShellManager(shell: IShell) : IShell by shell {
    companion object {
        lateinit var instance: ShellManager
    }
}