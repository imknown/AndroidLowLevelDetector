package net.imknown.android.forefrontinfo.base

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

interface IView {
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

    fun isActivityAndFragmentOk(fragment: Fragment) = with(fragment) {
        isAdded && activity != null
                && !activity!!.isFinishing
                && !activity!!.isDestroyed
    }

    companion object {
        const val fastScrollerClassPath = "androidx.recyclerview.widget.FastScroller"
    }

    @SuppressLint("PrivateResource")
    private fun createFastScrollerSingletonInstanceAndEnableIt(recyclerView: RecyclerView): Any {
        recyclerView.isVerticalScrollBarEnabled = false
        recyclerView.isHorizontalScrollBarEnabled = false

        val resources = recyclerView.context.resources
        return Class.forName(fastScrollerClassPath)
            .getDeclaredConstructor(
                RecyclerView::class.java,
                StateListDrawable::class.java,
                Drawable::class.java,
                StateListDrawable::class.java,
                Drawable::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java
            ).apply {
                isAccessible = true
            }.newInstance(
                recyclerView,
                ContextCompat.getDrawable(
                    recyclerView.context,
                    R.drawable.scroll_thumb_drawable
                ),
                ColorDrawable(Color.TRANSPARENT),
                ContextCompat.getDrawable(
                    recyclerView.context,
                    R.drawable.scroll_thumb_drawable
                ),
                ColorDrawable(Color.TRANSPARENT),
                resources.getDimensionPixelSize(androidx.recyclerview.R.dimen.fastscroll_default_thickness),
                resources.getDimensionPixelSize(androidx.recyclerview.R.dimen.fastscroll_minimum_range),
                resources.getDimensionPixelOffset(androidx.recyclerview.R.dimen.fastscroll_margin)
            )
    }

    fun setScrollBarMode(recyclerView: RecyclerView, scrollBarMode: String) {
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