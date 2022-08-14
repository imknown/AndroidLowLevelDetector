package net.imknown.android.forefrontinfo.ui

import android.content.pm.PackageManager
import androidx.annotation.IdRes
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.property.impl.ShizukuProperty
import rikka.shizuku.Shizuku

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val SAVED_STATE_HANDLE_KEY_LAST_ID = "SAVED_STATE_HANDLE_KEY_LAST_ID"
    }

    @IdRes
    var lastId = getSavedStateLastId()

    fun setSavedStateLastId(@IdRes id: Int) {
        lastId = id
        savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID] = id
    }

    @IdRes
    private fun getSavedStateLastId() = savedStateHandle[SAVED_STATE_HANDLE_KEY_LAST_ID]
        ?: R.id.navigation_home

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            removeRequestPermissionResultListener()

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                PropertyManager.instance = PropertyManager(ShizukuProperty)
            }
        }

    fun dealWithShiziku() {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            PropertyManager.instance = PropertyManager(ShizukuProperty)
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Doing nothing is OK
        } else {
            val shizukuFirstAskingKey =
                MyApplication.getMyString(R.string.function_shizuku_first_asking_key)
            val isShizukuFirstAsking =
                MyApplication.sharedPreferences.getBoolean(shizukuFirstAskingKey, true)
            if (!isShizukuFirstAsking) {
                return
            }

            MyApplication.sharedPreferences.edit {
                putBoolean(shizukuFirstAskingKey, false)
            }

            addRequestPermissionResultListener()

            Shizuku.requestPermission(0)
        }
    }

    private fun addRequestPermissionResultListener() {
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
    }

    fun removeRequestPermissionResultListener() {
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }
}