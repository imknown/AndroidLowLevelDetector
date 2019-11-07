package net.imknown.android.forefrontinfo.ui.home

data class Lld(
    val versionLocale: String,
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
        val upstreamMaster: Linux
    ) {
        data class Linux(
            val patchLevel: Int,
            val subLevel: Int,
            val extraVersion: String
        ) : BaseInfo()
    }

    data class Toyboxes(
        val stable: Toybox,
        val master: Toybox
    ) {
        class Toybox : BaseInfo()
    }
}