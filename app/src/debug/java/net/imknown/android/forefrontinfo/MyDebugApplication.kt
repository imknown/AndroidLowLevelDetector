package net.imknown.android.forefrontinfo

import android.os.StrictMode
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.LogRequestAsCurlInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import net.imknown.android.forefrontinfo.base.MyApplication

class MyDebugApplication : MyApplication() {
    override fun onCreate() {
        super.onCreate()

        StrictMode.enableDefaults()

        Shell.enableVerboseLogging = true

        FuelManager.instance.addRequestInterceptor(LogRequestAsCurlInterceptor)
        FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
        FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)

        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
    }
}