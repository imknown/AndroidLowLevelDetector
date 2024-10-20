package net.imknown.android.forefrontinfo.base.shell.impl

import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

object DefaultShell : IShell {
    override fun execute(cmd: String): ShellResult {
        val output = mutableListOf<String>()
        var exitCode: Int? = null
        var isSuccess = false

        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "$cmd\nexit"))
            process.waitFor()

            exitCode = process.exitValue()
            isSuccess = (exitCode == 0)

            output += if (isSuccess) {
                process.inputStream.bufferedReader().readLines()
            } else {
                process.errorStream.bufferedReader().readLines()
            }
        } catch (e: Exception) {
            output += (e.message ?: "")
        }

        return ShellResult(output, isSuccess, exitCode)
    }
}