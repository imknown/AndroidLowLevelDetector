package net.imknown.android.forefrontinfo.base.shell.impl

import net.imknown.android.forefrontinfo.base.shell.EMPTY_EXIT_CODE
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

object DefaultShell : IShell {
    override fun execute(cmd: String): ShellResult {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "$cmd\nexit"))
        process.waitFor()

        var output: List<String>
        var isSuccess: Boolean
        var exitCode: Int
        try {
            val err = process.errorStream.bufferedReader().readLines()
            if (err.isNotEmpty()) {
                output = err
                isSuccess = false
            } else {
                val out = process.inputStream.bufferedReader().readLines()
                output = out
                isSuccess = true
            }
            exitCode = process.exitValue()
        } catch (e: Exception) {
            output = emptyList()
            isSuccess = false
            exitCode = EMPTY_EXIT_CODE
        }

        return ShellResult(output, isSuccess, exitCode)
    }
}