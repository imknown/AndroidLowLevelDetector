package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of getOutdatedTargetSdkVersionApkState: the first API level property read (the raw
 * string, placeholder included, until the #42 rework), the outdated system APKs sorted per the
 * user's order preference, and the repository's match judgment. The sort order and [order] ride
 * together: the mapper picks the detail's list format from the same [order]. [match] means: any
 * outdated system APK present is [Match.None], an empty device with no lld reference is
 * [Match.Unknown], an empty device on the latest (stable or preview) Android is [Match.Met], and
 * an empty device below the latest is [Match.Lagging].
 */
data class OutdatedTargetSdkVersionApkState(
    val firstApiLevel: String,
    val outdatedApks: List<OutdatedApk>,
    val order: Order,
    val match: Match
) {
    data class OutdatedApk(
        val packageName: String,
        val targetSdkVersion: Int
    )

    enum class Order { PackageNameFirst, TargetSdkVersionFirst }

    enum class Match { Met, Lagging, Unknown, None }
}
