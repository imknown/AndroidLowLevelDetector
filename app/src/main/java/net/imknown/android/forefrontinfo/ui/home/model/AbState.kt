package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectAb: the master switch, whether virtual A/B is on, whether it is a
 * retrofit implementation, and the current slot suffix. The mapper builds the detail and
 * the color from them; the repository never touches display text. The slot suffix is null
 * when the property is absent or its read fails: the not-filled placeholder is display
 * material and stays out of the domain object.
 */
data class AbState(
    val enabled: Boolean,
    val virtualAb: Boolean,
    val virtualAbRetrofit: Boolean,
    val slotSuffix: String?
)
