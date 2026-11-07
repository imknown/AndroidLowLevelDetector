package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.ui.home.model.BaseInfo
import net.imknown.android.forefrontinfo.ui.home.model.Lld

// Minimal lld fixture for the per-entry domain tests: every required field gets a neutral
// default, tests override the slice they are exercising.
fun lldForTests(
    version: String = "2026-11-07T10:00:00+08:00",
    android: Lld.Androids = defaultAndroids(),
    linux: Lld.Linuxes = Lld.Linuxes(
        google = Lld.Linuxes.Versions(versions = emptyList()),
        mainline = BaseInfo(version = "6.12")
    ),
    toybox: Lld.Toyboxes = Lld.Toyboxes(
        stable = BaseInfo(version = "0.9.0"),
        support = BaseInfo(version = "0.8.0"),
        master = BaseInfo(version = "0.10.0")
    ),
    webView: Lld.WebViews = Lld.WebViews(
        stable = BaseInfo(version = "140.0"),
        beta = BaseInfo(version = "141.0")
    )
): Lld = Lld(
    scheme = 1,
    version = version,
    android = android,
    linux = linux,
    toybox = toybox,
    webView = webView
)

private fun defaultAndroids(): Lld.Androids {
    fun row(api: String, apiFull: String, version: String, name: String) =
        Lld.Androids.Android(api = api, apiFull = apiFull, version = version, name = name)

    return Lld.Androids(
        securityPatchLevel = "2026-11-01",
        googlePlaySystemUpdates = "2026-11-01",
        build = Lld.Androids.Build(
            version = "BP31.250705",
            details = listOf(Lld.Androids.Build.Detail(id = "BP31.250705", revision = "1"))
        ),
        stable = row("36", "36.1", "16", "Baklava"),
        stablePreview = row("37", "37.0", "17 QPR2", "CinnamonBun"),
        support = row("35", "35.0", "15", "VanillaIceCream"),
        preview = row("37", "37.1", "17 Canary", "CinnamonBun"),
        internal = row("37", "37.2", "17 Internal", "CinnamonBun"),
        known = emptyList()
    )
}
