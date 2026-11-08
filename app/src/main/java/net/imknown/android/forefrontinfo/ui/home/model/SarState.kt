package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectSar: whether the device runs system-as-root (SAR), and which shape it is.
 * [type] is present only when [supported] holds; a supported SAR that matches none of the four
 * known shapes lands on [SarType.Unknown] (the old else branch), rendering left to the mapper.
 */
data class SarState(
    val supported: Boolean,
    val type: SarType?
) {
    enum class SarType { Legacy, TwoSi, Recovery, Slash, Unknown }
}
