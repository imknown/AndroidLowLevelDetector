package net.imknown.android.forefrontinfo.base.shell.impl

import net.imknown.android.forefrontinfo.base.shell.EMPTY_EXIT_CODE
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

object DefaultShell : IShell {
    override fun execute(cmd: String): ShellResult {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "$cmd\nexit"))
        process.waitFor()

        var output: List<String>
        var exitCode: Int
        var isSuccess: Boolean
        try {
            val err = process.errorStream.bufferedReader().readLines()
            output = err.ifEmpty {
                process.inputStream.bufferedReader().readLines()
            }

            exitCode = process.exitValue()

            isSuccess = (exitCode == 0)
        } catch (e: Exception) {
            output = emptyList()
            isSuccess = false
            exitCode = EMPTY_EXIT_CODE
        }

        return ShellResult(output, isSuccess, exitCode)
    }
}