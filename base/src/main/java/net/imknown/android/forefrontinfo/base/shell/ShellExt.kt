package net.imknown.android.forefrontinfo.base.shell

fun getShellResultByCondition(cmd: String, condition: Boolean = true) = if (condition) {
    ShellManager.instance.execute(cmd)
} else {
    ShellResult()
}