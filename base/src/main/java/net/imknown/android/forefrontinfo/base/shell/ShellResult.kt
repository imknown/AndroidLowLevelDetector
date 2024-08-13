package net.imknown.android.forefrontinfo.base.shell

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ShellResult(
    val output: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val exitCode: Int = EMPTY_EXIT_CODE
): Parcelable {
    companion object {
        const val EMPTY_EXIT_CODE = -1000
    }
}