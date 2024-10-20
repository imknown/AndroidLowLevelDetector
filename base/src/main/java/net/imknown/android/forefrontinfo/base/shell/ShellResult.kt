package net.imknown.android.forefrontinfo.base.shell

class ShellResult(
    /** Stdout + Stderr */
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int? = null
)