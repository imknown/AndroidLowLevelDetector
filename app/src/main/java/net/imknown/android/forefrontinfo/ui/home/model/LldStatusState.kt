package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectMode: the lld payload version (null when the lld itself is missing),
 * where the payload came from, and the failures collected along the fetch chain as reasons
 * (the mapper renders their text).
 */
data class LldStatusState(
    val version: String?,
    val source: LldSource,
    val failures: List<LldFailure>
)
