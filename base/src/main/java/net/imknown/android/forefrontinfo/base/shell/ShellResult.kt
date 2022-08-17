package net.imknown.android.forefrontinfo.base.shell

class ShellResult(
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int = EMPTY_EXIT_CODE
) {
    companion object {
        const val EMPTY_EXIT_CODE = -1000
    }
}