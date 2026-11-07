package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectMode: the lld payload version (null when the lld itself is missing),
 * where the payload came from, and the already-rendered failure messages collected along the
 * fetch chain (they are ViewModel display material by origin, kept as-is here).
 */
data class LldStatusState(
    val version: String?,
    val source: LldSource,
    val errors: List<String>
)
