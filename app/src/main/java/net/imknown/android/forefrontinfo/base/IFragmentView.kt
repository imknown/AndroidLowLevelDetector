package net.imknown.android.forefrontinfo.base

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

interface IFragmentView : IView {

    fun showError(error: Throwable) {
        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
    }

    suspend fun toast(@StringRes resId: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, resId, Toast.LENGTH_LONG).show()
    }

    suspend fun toast(text: String) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, text, Toast.LENGTH_LONG).show()
    }

    suspend fun isActivityAndFragmentOk(fragment: Fragment) = withContext(Dispatchers.IO) {
        with(fragment) {
            isAdded && activity != null
                    && !activity!!.isFinishing
                    && !activity!!.isDestroyed
        }
    }

    suspend fun setScrollBarMode(recyclerView: RecyclerView, scrollBarMode: String) =
        withContext(Dispatchers.Main) {
            when (scrollBarMode) {
                MyApplication.getMyString(R.string.interface_no_scroll_bar_value) -> {
                    recyclerView.isVerticalScrollBarEnabled = false
                    recyclerView.isHorizontalScrollBarEnabled = false
                }
                MyApplication.getMyString(R.string.interface_normal_scroll_bar_value) -> {
                    recyclerView.isVerticalScrollBarEnabled = true
                    recyclerView.isHorizontalScrollBarEnabled = true
                }
                MyApplication.getMyString(R.string.interface_fast_scroll_bar_value) -> {
                    recyclerView.isVerticalScrollBarEnabled = false
                    recyclerView.isHorizontalScrollBarEnabled = false
                }
            }
        }
}