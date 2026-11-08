package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectMainline: the default module metadata provider package (null when the
 * framework's internal config resource is missing or its lookup failed, rendered as the
 * platform unknownName), the installed provider's version (null when the provider row was
 * never fetched for the same reasons, an empty string when the package exists without a
 * version, rendered verbatim), the lld's Google Play system updates date (null when the lld
 * is missing), and the match judgment (Met when the installed version reaches the lld's date
 * either directly or with a day appended, None otherwise).
 */
data class MainlineState(
    val moduleProvider: String?,
    val versionName: String?,
    val latestGooglePlaySystemUpdates: String?,
    val match: Match
) {
    enum class Match { Unknown, Met, None }
}
