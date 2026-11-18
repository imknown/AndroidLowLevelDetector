package net.imknown.android.forefrontinfo.ui.home.model

// Where the lld payload came from: fetched online, or read from the saved copy / the bundled
// asset by the fallback chain in LldLoader.load().
// Replaces the former use of R.string.lld_json_online as the branching value:
// resource IDs now only build the detail text.
enum class LldSource {
    ONLINE,
    OFFLINE
}
