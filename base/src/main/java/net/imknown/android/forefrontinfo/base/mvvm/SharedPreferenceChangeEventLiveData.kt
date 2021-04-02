package net.imknown.android.forefrontinfo.base.mvvm

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Copied from:
 * https://gist.github.com/rharter/1df1cd72ce4e9d1801bd2d49f2a96810
 */
abstract class SharedPreferenceChangeEventLiveData<T>(
    private val scope: CoroutineScope,
    protected val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T
) : LiveData<Event<T>>() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            scope.launch(Dispatchers.IO) {
                if (key == this@SharedPreferenceChangeEventLiveData.key) {
                    val event = getValueFromPreferences(key, defValue)

                    withContext(Dispatchers.Main) {
                        value = event
                    }
                }
            }
        }

    abstract suspend fun getValueFromPreferences(key: String, defValue: T): Event<T>

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

fun SharedPreferences.stringEventLiveData(scope: CoroutineScope, key: String, defValue: String) =
    object : SharedPreferenceChangeEventLiveData<String?>(scope, this, key, defValue) {
        override suspend fun getValueFromPreferences(key: String, defValue: String?) =
            Event(sharedPrefs.getString(key, defValue))
    }

fun SharedPreferences.booleanEventLiveData(scope: CoroutineScope, key: String, defValue: Boolean) =
    object : SharedPreferenceChangeEventLiveData<Boolean>(scope, this, key, defValue) {
        override suspend fun getValueFromPreferences(key: String, defValue: Boolean) =
            Event(sharedPrefs.getBoolean(key, defValue))
    }