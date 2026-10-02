package net.imknown.android.forefrontinfo.ui.common

import android.util.Log
import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.property.IProperty

// The fallback facade over IProperty: returns placeholder values when the
// condition is unmet or the reflective read throws, so the fallback logic
// lives in one place instead of being copied across the 33 call sites (the
// companion action of issues-cn #02, replacing the former top-level functions
// and their static-slot access). Byte-equivalent to the original: condition
// branches, try-catch, the result_not_supported / build_not_filled
// placeholders, and the log tags are all unchanged. IProperty lives in :base
// (the interface module stays Metro-free); MyApplication comes from the graph
// factory's @Provides parameter.
@Inject
class PropertyReader(
    private val property: IProperty,
    private val application: MyApplication
) {
    fun getString(key: String, condition: Boolean = true): String {
        val notSupport = application.getString(R.string.result_not_supported)
        return if (condition) {
            try {
                val default = application.getString(R.string.build_not_filled)
                property.getStringOrThrow(key, default)
            } catch (e: Exception) {
                Log.w("getStringProperty", "$key: ${e.fullMessage}")
                notSupport
            }
        } else {
            notSupport
        }
    }

    fun getBoolean(key: String, condition: Boolean = true) = if (condition) {
        try {
            property.getBooleanOrThrow(key, false)
        } catch (e: Exception) {
            Log.w("getBooleanProperty", "$key: ${e.fullMessage}")
            false
        }
    } else {
        false
    }
}
