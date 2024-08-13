> https://github.com/RikkaApps/Shizuku-API/blob/master/README.md
> https://github.com/RikkaApps/Shizuku-API/tree/master/demo

```kotlin
object ShizukuProperty : IProperty {
    override fun getString(key: String, default: String): String =
        ShizukuSystemProperties.get(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        ShizukuSystemProperties.getBoolean(key, default)
}
```

---

```kotlin
private val requestPermissionResultListener =
    Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        removeRequestPermissionResultListener()

        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            PropertyManager.instance = PropertyManager(ShizukuProperty)
        }
    }

fun dealWithShizuku() {
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
```

---

> /Users/he.jin/Desktop/imknown/Projects/AS/Open/AndroidLowLevelDetector/gradle/toml/thirdParty.toml

```toml
[versions]

# https://github.com/RikkaApps/Shizuku-API#add-dependency
shizuku = "13.1.5"

[libraries]

# ...

shizuku-api = { group = "dev.rikka.shizuku", name = "api", version.ref = "shizuku" }
shizuku-provider = { group = "dev.rikka.shizuku", name = "provider", version.ref = "shizuku" }

[bundles]
shizuku = ["shizuku-api", "shizuku-provider"]
```

> build.gradle.kts
```kotlin
implementation(libsThirdParty.bundles.shizuku)
```

---

```kotlin
object ShizukuProperty : IProperty {
    override fun getString(key: String, default: String): String =
        ShizukuSystemProperties.get(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        ShizukuSystemProperties.getBoolean(key, default)
}
```

---

```xml
<string name="function_shizuku_first_asking_key" translatable="false">function_shizuku_first_asking_key</string>
```

---

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:enabled="true"
    android:exported="true"
    android:multiprocess="false"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

---

```kotlin
var userService: IUserService? = null

val state = MutableStateFlow<Boolean?>(null)

private fun dealWithShizuku() {
    if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
        state.value = false
        return
    }

    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        bindShizukuUserService()
    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
        // User denied permission
        state.value = false
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
            state.value = false
        }
    }

private fun bindShizukuUserService() {
    PropertyManager.Companion.instance = PropertyManager(ShizukuProperty)

    try {
        if (Shizuku.getVersion() < 10) {
            Log.i("bindUserService", "requires Shizuku API 10")
            state.value = false
        } else {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        }
    } catch (tr: Throwable) {
        tr.printStackTrace()
        state.value = false
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
                state.value = true
            }  catch (e: RemoteException) {
                e.printStackTrace()
                state.value = false
            }
        } else {
            Log.e("zzz", "Invalid binder for $componentName")
            state.value = false
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

protected fun sh(cmd: String, condition: Boolean = true): ShellResult {
    return if (condition) {
        MyApplication.userService?.execute(cmd)
            ?: ShellManager.instance.execute(cmd)
    } else {
        ShellResult()
    }
}
```