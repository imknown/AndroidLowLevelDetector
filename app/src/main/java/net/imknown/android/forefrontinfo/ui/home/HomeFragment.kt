package net.imknown.android.forefrontinfo.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.IUserService
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.ShellResult
import net.imknown.android.forefrontinfo.base.shell.impl.LibSuShell
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.base.list.MyAdapter
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import kotlin.system.exitProcess

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    override val listViewModel by viewModels<HomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.homeLanguageEvent)

        listViewModel.subtitle.observe(viewLifecycleOwner) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.subtitle = MyApplication.getMyString(it.lldDataModeResId, it.dataVersion)
        }

        listViewModel.outdatedOrderProp.observe(viewLifecycleOwner, EventObserver {
            listViewModel.payloadOutdatedTargetSdkVersionApk(myAdapter.myModels)
        })

        listViewModel.showOutdatedOrderEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyItemChanged(myAdapter.myModels.lastIndex, MyAdapter.PAYLOAD_DETAILS)
        })

        try {
            if (Shizuku.getVersion() < 10) {
                Log.i("bindUserService", "requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
        }

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

    override fun onDestroy() {
        super.onDestroy()

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
            val a = ShellManager.instance.execute(command)
            return a
        }
    }

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.getName())
    ).daemon(false).processNameSuffix("service").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                MyApplication.userService = IUserService.Stub.asInterface(binder)
                try {
                    // Process: net.imknown.android.forefrontinfo.debug
                    // Thread: main
                }  catch (e: RemoteException) {
                    e.printStackTrace()
                }
            } else {
                Log.e("zzz", "Invalid binder for $componentName")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("zzz", "onServiceDisconnected: $componentName")
        }
    }
}
