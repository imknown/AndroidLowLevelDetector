package net.imknown.android.forefrontinfo.base.shell.impl

import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

object ShellDefault : IShell {
    override fun execute(cmd: String): ShellResult {
        val output = mutableListOf<String>()
        var exitCode: Int? = null
        var isSuccess = false

        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "$cmd\nexit"))
            process.waitFor()

            exitCode = process.exitValue()
            isSuccess = (exitCode == 0)

            val stream = with(process) { if (isSuccess) inputStream else errorStream }
            output += stream.bufferedReader().readLines()
        } catch (e: Exception) {
            output += e.fullMessage
        }

        return ShellResult(output, isSuccess, exitCode)
    }
}