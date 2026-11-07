package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectDynamicPartitions: the master switch and whether it is a retrofit
 * implementation. Retrofit only affects the detail's appended text; enabled drives the
 * two states and the color.
 */
data class DynamicPartitionsState(
    val enabled: Boolean,
    val retrofit: Boolean
)
