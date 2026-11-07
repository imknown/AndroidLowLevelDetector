package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectKernel: the raw os.version string (an absent value fails the detection
 * in the repository, a deliberate change recorded in the batch 4 spec notes; a non-null
 * string goes through the version library's lenient parse, so a non-numeric string compares
 * as 0.0 rather than failing), the lld's supported kernel versions (null when the lld is
 * missing), the lld's mainline version, and the repository's match judgment (the last
 * major+minor match in the lld order wins, as before).
 */
data class KernelState(
    val linuxVersionString: String,
    val supportedVersions: List<String>?,
    val mainlineVersion: String?,
    val match: KernelMatch
) {
    enum class KernelMatch { Met, Lagging, None }
}
