package net.imknown.android.forefrontinfo.ui.home.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
open class BaseInfo {
    lateinit var version: String
}
