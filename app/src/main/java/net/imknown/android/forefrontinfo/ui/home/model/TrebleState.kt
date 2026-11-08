package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectTreble: whether the Treble switch is on, and where the vendor partition's
 * vintf manifest lives when it is. [vintf] is probed only while the switch is on, null means
 * disabled. [Vintf.Other] is the old else branch: none of the manifest paths exists.
 */
data class TrebleState(
    val enabled: Boolean,
    val vintf: Vintf?
) {
    enum class Vintf { Present, LegacyNoFragments, Other }
}
