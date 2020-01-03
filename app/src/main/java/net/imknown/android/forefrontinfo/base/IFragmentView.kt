package net.imknown.android.forefrontinfo.base

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

interface IFragmentView {

    suspend fun toast(@StringRes resId: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, resId, Toast.LENGTH_LONG).show()
    }

    suspend fun toast(text: String) = withContext(Dispatchers.Main) {
        Toast.makeText(MyApplication.instance, text, Toast.LENGTH_LONG).show()
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