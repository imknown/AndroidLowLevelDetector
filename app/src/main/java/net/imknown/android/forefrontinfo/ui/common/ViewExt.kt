package net.imknown.android.forefrontinfo.ui.common

import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import net.imknown.android.forefrontinfo.R

@MainThread
fun RecyclerView.setScrollBarMode(scrollBarMode: String?) {
    isVerticalScrollBarEnabled = when (scrollBarMode) {
        context.getString(R.string.interface_normal_scroll_bar_value) -> true
        // null,
        // R.string.interface_no_scroll_bar_value,
        // R.string.interface_fast_scroll_bar_value,
        else -> false
    }
}