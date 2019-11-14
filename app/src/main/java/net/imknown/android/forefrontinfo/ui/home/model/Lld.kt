package net.imknown.android.forefrontinfo.ui.home.model

data class Lld(
    val android: Androids,
    val linux: Types<BaseInfo>,
    val toybox: Types<BaseInfo>
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

    data class Types<T>(
        val stable: T,
        val support: T,
        val master: T
    ) : BaseInfo() where T : BaseInfo
}