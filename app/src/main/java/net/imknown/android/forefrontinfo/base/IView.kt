package net.imknown.android.forefrontinfo.base

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication

interface IView {
    fun showNetError(error: Throwable) {
        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
    }

    suspend fun toast(@StringRes resId: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, resId, Toast.LENGTH_LONG).show()
    }

    suspend fun toast(text: String?) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, text, Toast.LENGTH_LONG).show()
    }

    fun isActivityAndFragmentOk(fragment: Fragment) = with(fragment) {
        isAdded && activity != null
                && !activity!!.isFinishing
                && !activity!!.isDestroyed
    }
}