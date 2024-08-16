package net.imknown.android.forefrontinfo

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Environment
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.imknown.android.forefrontinfo.base.IUserService
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.property.impl.DefaultProperty
import net.imknown.android.forefrontinfo.base.property.impl.ShizukuProperty
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.impl.LibSuShell
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import java.io.File
import net.imknown.android.forefrontinfo.base.R as baseR

open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

        var userService: IUserService? = null

        val homeLanguageEvent: LiveData<Event<Unit>> by lazy { instance._homeLanguageEvent }
        val othersLanguageEvent: LiveData<Event<Unit>> by lazy { instance._othersLanguageEvent }
        val propLanguageEvent: LiveData<Event<Unit>> by lazy { instance._propLanguageEvent }

        val sharedPreferences: SharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(instance)
        }

        fun getDownloadDir() = getFileDir(Environment.DIRECTORY_DOWNLOADS)

        private fun getFileDir(type: String): File {
            val externalFilesDir = instance.getExternalFilesDir(type)
            return if (externalFilesDir != null && externalFilesDir.exists()) {
                externalFilesDir
            } else {
                instance.filesDir.resolve(type)
            }.apply {
                mkdirs()
            }
        }

        fun getMyString(@StringRes resId: Int) =
            instance.getString(resId)

        fun getMyString(@StringRes resId: Int, vararg formatArgs: Any?) =
            instance.getString(resId, *formatArgs)

        fun setMyTheme(themesValue: String) {
            @AppCompatDelegate.NightMode val mode = when (themesValue) {
                getMyString(baseR.string.interface_themes_follow_system_value) -> {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                getMyString(baseR.string.interface_themes_power_saver_value) -> {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
                getMyString(baseR.string.interface_themes_always_light_value) -> {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                getMyString(baseR.string.interface_themes_always_dark_value) -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                else -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
            }

            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private val _homeLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }
    private val _othersLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }
    private val _propLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }

    override fun onCreate() {
        super.onCreate()

        instance = this@MyApplication

        initTheme()

        initShell()

        initLanguage()

        dealWithShizuku()
    }

    private fun initTheme() {
        DynamicColors.applyToActivitiesIfAvailable(this)

        val themesValue = sharedPreferences.getString(
            getMyString(baseR.string.interface_themes_key),
            getMyString(baseR.string.interface_themes_follow_system_value)
        )!!
        setMyTheme(themesValue)
    }

    private fun initShell() {
        // Shell.enableVerboseLogging = BuildConfig.DEBUG // Has been moved to Debug BuildType
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_NON_ROOT_SHELL)
        )

        ShellManager.instance = ShellManager(LibSuShell)
        PropertyManager.instance = PropertyManager(DefaultProperty)
    }

    private fun initLanguage() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                _homeLanguageEvent.value = Event(Unit)
                _othersLanguageEvent.value = Event(Unit)
                _propLanguageEvent.value = Event(Unit)
            }
        }

        registerReceiver(receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }

    private val _state = MutableStateFlow<Boolean?>(null)
    val state: StateFlow<Boolean?> = _state

    private fun dealWithShizuku() {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
            _state.value = false
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizukuUserService()
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // User denied permission
            _state.value = false
        } else {
            addRequestPermissionResultListener()

            Shizuku.requestPermission(0)
        }
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            removeRequestPermissionResultListener()

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindShizukuUserService()
            } else {
                _state.value = false
            }
        }

    private fun bindShizukuUserService() {
        PropertyManager.instance = PropertyManager(ShizukuProperty)

        try {
            if (Shizuku.getVersion() < 10) {
                Log.i("bindUserService", "requires Shizuku API 10")
                _state.value = false
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            _state.value = false
        }
    }

    private fun checkShizukuStatue() {
        try {
            if (Shizuku.getVersion() < 12) {
                Log.i("peekUserService", "requires Shizuku API 12")
            } else {
                val serviceVersion = Shizuku.peekUserService(userServiceArgs, userServiceConnection)
                if (serviceVersion != -1) {
                    Log.i("peekUserService", "Service is running, version $serviceVersion")
                } else {
                    Log.i("peekUserService", "Service is not running")
                }
            }
        } catch (tr: Throwable ) {
            tr.printStackTrace()
        }
    }

    private fun addRequestPermissionResultListener() {
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
    }

    private fun removeRequestPermissionResultListener() {
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.getName())
    ).daemon(false).processNameSuffix("service").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                userService = IUserService.Stub.asInterface(binder)
                try {
                    // Process: net.imknown.android.forefrontinfo.debug
                    // Thread: main
                    _state.value = true
                }  catch (e: RemoteException) {
                    e.printStackTrace()
                    _state.value = false
                }
            } else {
                Log.e("zzz", "Invalid binder for $componentName")
                _state.value = false
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("zzz", "onServiceDisconnected: $componentName")
        }
    }

    private fun destroyShizuku() {
        removeRequestPermissionResultListener()

        try {
            if (Shizuku.getVersion() < 10) {
                Log.i("unbindUserService", "unbindUserService requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
        }
    }
}