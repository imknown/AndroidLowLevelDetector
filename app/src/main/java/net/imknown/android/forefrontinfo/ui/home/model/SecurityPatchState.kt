package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of one detectSecurityPatches row: the patch value (raw: Build.VERSION.SECURITY_PATCH
 * for the system row, the vendor property read for the vendor row; an empty or placeholder
 * value counts as not present in the judgment until the #42 rework), the lld's declared
 * security patch level (null when the lld is missing), and the match judgment (Met when the
 * patch is at or past the level, Lagging when only the year-month matches, None otherwise,
 * including a not-present value).
 */
data class SecurityPatchState(
    val patch: String,
    val lldPatchLevel: String?,
    val match: Match
) {
    enum class Match { Unknown, Met, Lagging, None }
}
