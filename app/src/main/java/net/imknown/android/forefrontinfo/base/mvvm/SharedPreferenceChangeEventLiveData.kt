package net.imknown.android.forefrontinfo.base.mvvm

import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Copied from:
 * https://gist.github.com/rharter/1df1cd72ce4e9d1801bd2d49f2a96810
 */
abstract class SharedPreferenceChangeEventLiveData2<T>(
    private val scope: CoroutineScope,
    protected val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T
) : DefaultLifecycleObserver {
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            scope.launch {
                if (key != this@SharedPreferenceChangeEventLiveData2.key) {
                    return@launch
                }

                val event = withContext(Dispatchers.Default) {
                    getValueFromPreferences(key, defValue)
                }
//                value = Event(event)
            }
        }

    abstract suspend fun getValueFromPreferences(key: String, defValue: T): T

    override fun onCreate(owner: LifecycleOwner) {
        // value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

fun SharedPreferences.stringEventLiveData2(scope: CoroutineScope, key: String, defValue: String) =
    object : SharedPreferenceChangeEventLiveData2<String?>(scope, this, key, defValue) {
        override suspend fun getValueFromPreferences(key: String, defValue: String?) =
            sharedPrefs.getString(key, defValue)
    }

fun SharedPreferences.booleanEventLiveData2(scope: CoroutineScope, key: String, defValue: Boolean) =
    object : SharedPreferenceChangeEventLiveData2<Boolean>(scope, this, key, defValue) {
        override suspend fun getValueFromPreferences(key: String, defValue: Boolean) =
            sharedPrefs.getBoolean(key, defValue)
    }