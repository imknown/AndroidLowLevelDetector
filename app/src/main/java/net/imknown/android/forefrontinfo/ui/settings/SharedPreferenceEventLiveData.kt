package net.imknown.android.forefrontinfo.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import net.imknown.android.forefrontinfo.base.SingleEvent

/**
 * Copied from:
 * https://gist.github.com/rharter/1df1cd72ce4e9d1801bd2d49f2a96810
 */
abstract class SharedPreferenceEventLiveData<T>(
    protected val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T
) : LiveData<SingleEvent<T>>() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == this.key) {
                value = getValueFromPreferences(key, defValue)
            }
        }

    abstract fun getValueFromPreferences(key: String, defValue: T): SingleEvent<T>

    override fun onActive() {
        super.onActive()
        // value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

// TODO: Need async
fun SharedPreferences.stringEventLiveData(key: String, defValue: String) =
    object : SharedPreferenceEventLiveData<String?>(this, key, defValue) {
        override fun getValueFromPreferences(key: String, defValue: String?) =
            SingleEvent(sharedPrefs.getString(key, defValue))
    }

// TODO: Consider async to avoid StrictMode
fun SharedPreferences.booleanEventLiveData(key: String, defValue: Boolean) =
    object : SharedPreferenceEventLiveData<Boolean>(this, key, defValue) {
        override fun getValueFromPreferences(key: String, defValue: Boolean) =
            SingleEvent(sharedPrefs.getBoolean(key, defValue))
    }