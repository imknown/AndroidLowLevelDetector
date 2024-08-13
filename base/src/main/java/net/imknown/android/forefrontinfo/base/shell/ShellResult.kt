package net.imknown.android.forefrontinfo.base.shell

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ShellResult(
    /** Stdout + Stderr */
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int? = null
) : Parcelable