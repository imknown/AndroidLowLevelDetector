package net.imknown.android.forefrontinfo

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import net.imknown.android.forefrontinfo.base.IUserService
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.ShellResult
import net.imknown.android.forefrontinfo.base.shell.impl.LibSuShell
import kotlin.system.exitProcess

/** Invoke on process `system_process` */
class UserService : IUserService.Stub {
    /**
     * Constructor is required.
     */
    @Suppress("unused")
    constructor() {
        Log.i("UserService", "constructor")
    }

    /**
     * Constructor with Context. This is only available from Shizuku API v13.
     *
     * This method need to be annotated with [Keep] to prevent ProGuard from removing it.
     *
     * @param context Context created with createPackageContextAsUser
     * @see [code used to create the instance of this class](https://github.com/RikkaApps/Shizuku-API/blob/672f5efd4b33c2441dbf609772627e63417587ac/server-shared/src/main/java/rikka/shizuku/server/UserService.java.L66)
     */
    @Suppress("unused")
    @Keep
    constructor(context: Context) {
        Log.i("UserService", "constructor with Context: context=$context")
    }

    /** Reserved destroy method */
    override fun destroy() {
        Log.i("UserService", "destroy")
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    override fun execute(command: String): ShellResult {
        ShellManager.instance = ShellManager(LibSuShell)
        return ShellManager.instance.execute(command)
    }
}