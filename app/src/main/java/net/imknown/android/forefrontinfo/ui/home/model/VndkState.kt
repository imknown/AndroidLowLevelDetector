package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectVndk: the built-in VNDK version (null when the property is absent or the
 * read fails, the absence-aware read), whether the VNDK Lite variant is on (only read when the
 * version exists, false otherwise), and the match judgment (Met when the device tracks the
 * preview channel or its VNDK version reaches the lld's stable API without the Lite variant,
 * Lagging when the version exists but falls short, None when there is no version, Unknown when
 * the lld is missing).
 */
data class VndkState(
    val vndkVersion: String?,
    val hasVndkLite: Boolean,
    val match: Match
) {
    enum class Match { Unknown, Met, Lagging, None }
}
