package net.imknown.android.forefrontinfo.base.shell

class ShellResult(
    /** Stdout only; stderr goes to [errors]. ShellLibSu (the live producer) honors this; unused ShellDefault is the exception, fix its deadlock before enabling */
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int? = null,
    /** Stderr only; kept separate from [output] so parsers never see mixed lines (ShellDefault is the exception) */
    val errors: List<String> = emptyList()
)