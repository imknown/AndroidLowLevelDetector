package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectBuildId: the device's own Build.ID and the three partition build ids
 * (raw property reads: a failed or unmet read carries the localized placeholder it produced,
 * which the detail renders verbatim until the #42 rework of the value-presence judgment),
 * the lld's declared build version and per-partition details (null when the lld is missing),
 * and the freshness judgment (the device build date compared against the lld's first detail,
 * with the preview-channel offset; Lagging means the device runs a latest channel yet its
 * build predates the lld's).
 */
data class BuildIdState(
    val buildId: String,
    val systemBuildId: String,
    val vendorBuildId: String,
    val odmBuildId: String,
    val lldBuildVersion: String?,
    val lldDetails: List<BuildDetail>?,
    val match: Match
) {
    data class BuildDetail(val id: String, val revision: String)

    enum class Match { Unknown, Met, Lagging, Outdated }
}
