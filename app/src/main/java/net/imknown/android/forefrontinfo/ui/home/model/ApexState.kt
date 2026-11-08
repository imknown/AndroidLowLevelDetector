package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectApex: whether APEX is updatable (ro.apex.updatable), and whether a
 * flattened APEX is mounted. The mapper combines the two facts into updatable / legacy
 * flattened only / neither.
 */
data class ApexState(
    val updatable: Boolean,
    val flattenedMounted: Boolean
)
