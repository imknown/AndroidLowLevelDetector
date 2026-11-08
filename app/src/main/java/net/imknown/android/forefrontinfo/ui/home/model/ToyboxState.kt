package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectToybox: the device's toybox read, the three lld reference versions, and the
 * repository's match judgment. The read has three shapes: [ToyboxRead.Version] is the raw stdout
 * first line (the "toybox " prefix included, rendered verbatim by the mapper),
 * [ToyboxRead.NoOutput] is a succeeded command with no output (rendered as the platform
 * unknown), and [ToyboxRead.CommandFailed] is a failed command (rendered as not supported);
 * [match] compares versions only on a real read, the other two shapes land on [Match.None].
 */
data class ToyboxState(
    val read: ToyboxRead,
    val stableVersion: String?,
    val supportVersion: String?,
    val masterVersion: String?,
    val match: Match
) {
    sealed interface ToyboxRead {
        data class Version(val value: String) : ToyboxRead
        data object NoOutput : ToyboxRead
        data object CommandFailed : ToyboxRead
    }

    enum class Match { Met, Lagging, None }
}
