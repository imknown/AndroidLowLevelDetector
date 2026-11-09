package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectWebView: the built-in provider candidates (read via the hidden
 * WebViewUpdateService), the current implementation package, the lld reference versions, and the
 * repository's match judgment. A provider's [BuiltInProvider.packageName] or
 * [BuiltInProvider.description] is null when the hidden-API reflection for that field failed and
 * the mapper renders the not-filled placeholder. [Implement] carries the raw package read: null
 * fields render as the literal "null" in the detail, exactly like the old string template.
 * [match] compares versions only against a present lld stable version: the built-in candidates
 * meeting it is [Match.Met], only the implementation meeting it is [Match.Lagging], and
 * everything else (no lld version included) is [Match.None].
 */
data class WebViewState(
    val isQOrLater: Boolean,
    val builtInProviders: List<BuiltInProvider>,
    val implement: Implement?,
    val stableVersion: String?,
    val betaVersion: String?,
    val match: Match
) {
    data class BuiltInProvider(
        val packageName: String?,
        val description: String?,
        val availableByDefault: Boolean,
        val isFallback: Boolean,
        val hasSignatures: Boolean,
        val installState: InstallState
    )

    sealed interface InstallState {
        // [versionName] is null when the installed package carries no version; the mapper
        // renders the platform unknown for it.
        data class Installed(val versionName: String?) : InstallState
        data object NotInstalled : InstallState
    }

    data class Implement(
        val packageName: String?,
        val label: CharSequence?,
        val versionName: String?
    )

    enum class Match { Met, Lagging, None }
}
