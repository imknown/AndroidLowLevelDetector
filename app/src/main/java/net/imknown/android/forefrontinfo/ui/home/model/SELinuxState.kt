package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectSELinux: the device's SELinux mode as reported by the `getenforce` command.
 * A failed command whose first output line ends with the permission-denied marker still reports
 * [Mode.Enforcing] (the old path's reading: no permission to even query means enforcing);
 * every other unreadable shape lands on [Mode.Unknown], rendering left to the mapper.
 */
data class SELinuxState(
    val mode: Mode
) {
    enum class Mode { Enforcing, Permissive, Disabled, Unknown }
}
