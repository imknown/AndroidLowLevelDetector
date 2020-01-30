package net.imknown.android.forefrontinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import net.imknown.android.forefrontinfo.base.Event

class LanguageBroadcastLiveData : LiveData<Event<Int>>() {
    override fun onActive() {
        super.onActive()

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
            value = Event(0)
        }
    }
}