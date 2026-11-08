package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectGsi: whether the linkerconfig vendor namespace is isolated. The old path
 * drew the unidentified/not-supported boundary from the treble row's conclusion (unidentified
 * only when treble is supported and API >= P); that cross-entry judgment stays in the
 * repository, rendering left to the mapper.
 */
data class GsiState(
    val compatibility: Compatibility
) {
    enum class Compatibility { Compatible, NotCompatible, Unidentified, NotSupported }
}
