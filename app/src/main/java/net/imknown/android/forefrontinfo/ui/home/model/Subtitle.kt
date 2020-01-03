package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.StringRes

data class Subtitle(
    @StringRes val lldDataModeResId: Int,
    val dataVersion: String
)