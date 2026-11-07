package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectAndroid. The version is the known-row corrected one without the preview
 * suffix (the suffix is display material and the mapper adds it); the dessert is null when
 * unknown; [canaryLabel] is the pascal-cased codename fact and only set when the device runs a
 * canary; the currency is the repository's freshness judgment over the lld.
 */
data class AndroidInfoState(
    val version: String,
    val dessert: String?,
    val isPreview: Boolean,
    val canaryLabel: String?,
    val isGoEdition: Boolean,
    val apiFull: String,
    val referenceRows: List<AndroidReferenceRow>,
    val currency: AndroidCurrency
) {
    /**
     * One of the five upstream reference lines (stable, support, stable preview, preview,
     * internal); both fields are null only when the whole lld is missing, otherwise they are
     * the lld's declared values.
     */
    data class AndroidReferenceRow(val version: String?, val apiFull: String?)

    /** The freshness judgment; Unknown means the lld itself was missing. */
    enum class AndroidCurrency { Unknown, Latest, Supported, Outdated }
}
