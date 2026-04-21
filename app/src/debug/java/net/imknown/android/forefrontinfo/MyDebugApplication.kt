package net.imknown.android.forefrontinfo

import android.os.StrictMode
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import leakcanary.LeakCanaryProcess
import net.imknown.android.forefrontinfo.base.MyApplication

class MyDebugApplication : MyApplication() {
    override fun onCreate() {
        if (LeakCanaryProcess.isInAnalyzerProcess(this)) {
            return
        }

        super.onCreate()

        StrictMode.enableDefaults()

        Shell.enableVerboseLogging = true

        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
    }
}