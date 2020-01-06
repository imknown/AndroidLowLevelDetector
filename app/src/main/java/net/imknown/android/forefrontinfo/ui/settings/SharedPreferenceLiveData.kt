package net.imknown.android.forefrontinfo.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

/**
 * Copied from:
 * https://gist.github.com/rharter/1df1cd72ce4e9d1801bd2d49f2a96810
 */
abstract class SharedPreferenceLiveData<T>(
    protected val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T
) : LiveData<T>() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == this.key) {
                value = getValueFromPreferences(key, defValue)
            }
        }

    abstract fun getValueFromPreferences(key: String, defValue: T): T?

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

fun SharedPreferences.stringLiveData(key: String, defValue: String) =
    object : SharedPreferenceLiveData<String>(this, key, defValue) {
        override fun getValueFromPreferences(key: String, defValue: String) =
            sharedPrefs.getString(key, defValue)
    }

fun SharedPreferences.booleanLiveData(key: String, defValue: Boolean) =
    object : SharedPreferenceLiveData<Boolean>(this, key, defValue) {
        override fun getValueFromPreferences(key: String, defValue: Boolean) =
            sharedPrefs.getBoolean(key, defValue)
    }