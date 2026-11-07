package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectSdkExtension: the device's current extension version (null below the
 * level that declares one), the lld's stable extension (null when undeclared), and the
 * repository's judgment whether the current one meets the stable bar.
 */
data class SdkExtensionState(
    val currentExtension: Int?,
    val stableExtension: Int?,
    val meetsStable: Boolean
)
