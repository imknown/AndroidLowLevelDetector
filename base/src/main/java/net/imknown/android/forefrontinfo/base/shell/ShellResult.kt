package net.imknown.android.forefrontinfo.base.shell

const val EMPTY_EXIT_CODE = -1000

class ShellResult(
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int = EMPTY_EXIT_CODE
)