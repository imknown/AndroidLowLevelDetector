package net.imknown.android.forefrontinfo.base

import android.app.Application
import android.content.SharedPreferences
import android.os.Environment
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.topjohnwu.superuser.Shell
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.property.impl.PropertyDefault
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.ui.common.ShellLibSu
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
import java.io.File

open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

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
    }

    override fun onCreate() {
        super.onCreate()

        instance = this@MyApplication

        initMyAndroid()

        initShellAndProperty()
    }

    private fun initShellAndProperty() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.enableLegacyStderrRedirection = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_NON_ROOT_SHELL)
//                .setInitializers(Shell.Initializer::class.java)
        )

        ShellManager.instance = ShellManager(ShellLibSu)

        PropertyManager.instance = PropertyManager(PropertyDefault)
    }
}