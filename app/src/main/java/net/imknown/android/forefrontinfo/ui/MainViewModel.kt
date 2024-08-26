package net.imknown.android.forefrontinfo.ui

import androidx.annotation.IdRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import net.imknown.android.forefrontinfo.R

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

//    private val requestPermissionResultListener =
//        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
//            removeRequestPermissionResultListener()
//
//            if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                PropertyManager.instance = PropertyManager(ShizukuProperty)
//            }
//        }
//
//    fun dealWithShizuku() {
//        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
//            return
//        }
//
//        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
//            PropertyManager.instance = PropertyManager(ShizukuProperty)
//        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
//            // Doing nothing is OK
//        } else {
//            val shizukuFirstAskingKey =
//                MyApplication.getMyString(R.string.function_shizuku_first_asking_key)
//            val isShizukuFirstAsking =
//                MyApplication.sharedPreferences.getBoolean(shizukuFirstAskingKey, true)
//            if (!isShizukuFirstAsking) {
//                return
//            }
//
//            MyApplication.sharedPreferences.edit {
//                putBoolean(shizukuFirstAskingKey, false)
//            }
//
//            addRequestPermissionResultListener()
//
//            Shizuku.requestPermission(0)
//        }
//    }
//
//    private fun addRequestPermissionResultListener() {
//        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
//    }
//
//    fun removeRequestPermissionResultListener() {
//        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
//    }
}