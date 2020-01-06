package net.imknown.android.forefrontinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData

class LanguageBroadcastLiveData : LiveData<Nothing>() {
    override fun onActive() {
        super.onActive()

        value = null

        MyApplication.instance.registerReceiver(
            receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        )
    }

    override fun onInactive() {
        super.onInactive()
        MyApplication.instance.unregisterReceiver(receiver)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            value = null
        }
    }
}