package net.imknown.android.forefrontinfo.ui.home.model

/**
 * One failure along the lld fetch chain, kept as a reason plus the underlying message: the mapper
 * renders the user-facing line, so the loader never assembles display text (the same split the
 * detection entries follow: the data layer returns reasons, the mapper renders them).
 */
data class LldFailure(val reason: LldFailureReason, val causeMessage: String)

/** Where along the chain the failure happened; each value maps to one existing message. */
enum class LldFailureReason {
    /** The online payload could not be fetched. */
    Fetch,

    /** A payload was fetched or read but could not be parsed into an [Lld]. */
    Parse,

    /** A payload parsed but could not be written to (or copied into) the private files dir. */
    Save
}
