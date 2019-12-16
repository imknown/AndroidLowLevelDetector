package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.Keep

@Keep
data class Lld(
    val android: Androids,
    val linux: Linuxes,
    val toybox: Toyboxes,
    val webView: WebViews
) : BaseInfo() {
    @Keep
    data class Androids(
        val securityPatchLevel: String,
        val build: Build,
        val stable: Android,
        val support: Android,
        val alpha: Android,
        val beta: Android
    ) {
        @Keep
        data class Android(
            val active: Boolean,
            val codename: String,
            val api: String
        ) : BaseInfo()

        @Keep
        data class Build(
            val details: List<Detail>
        ) : BaseInfo() {
            @Keep
            data class Detail(
                val id: String,
                val revision: String
            )
        }
    }

    @Keep
    data class Linuxes(
        val stable: BaseInfo,
        val support: BaseInfo,
        val mainline: BaseInfo
    ) : BaseInfo()

    @Keep
    data class Toyboxes(
        val stable: BaseInfo,
        val support: BaseInfo,
        val mainline: BaseInfo,
        val master: BaseInfo
    ) : BaseInfo()

    @Keep
    data class WebViews(
        val stable: BaseInfo,
        val beta: BaseInfo,
        val dev: BaseInfo,
        val canary: BaseInfo
    )
}