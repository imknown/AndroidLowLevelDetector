package net.imknown.android.forefrontinfo.ui.home.model

data class Lld(
    val android: Androids,
    val linux: Linuxes,
    val toybox: Toyboxes
) : BaseInfo() {
    data class Androids(
        val securityPatchLevel: String,
        val stable: Android,
        val support: Android,
        val alpha: Android,
        val beta: Android
    ) {
        data class Android(
            val active: Boolean,
            val codename: String,
            val api: String
        ) : BaseInfo()
    }

    data class Linuxes(
        val stable: Linux,
        val support: Linux,
        val upstreamMaster: Linux
    ) {
        class Linux : BaseInfo()
    }

    data class Toyboxes(
        val stable: Toybox,
        val master: Toybox
    ) {
        class Toybox : BaseInfo()
    }
}