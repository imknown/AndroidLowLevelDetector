package net.imknown.android.forefrontinfo.ui.base.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.imknown.android.forefrontinfo.R

@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    scrollBarMode: String?,
    width: Dp = 4.dp
): Modifier {
    val isEnabled = scrollBarMode == stringResource(R.string.interface_normal_scroll_bar_value)
    
    if (!isEnabled) return this

    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    
    return this.drawWithContent {
        drawContent()
        drawVerticalScrollbar(state, width.toPx(), color)
    }
}

private fun ContentDrawScope.drawVerticalScrollbar(
    state: LazyListState,
    widthPx: Float,
    color: Color
) {
    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isEmpty()) {
        return
    }

    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsCount = visibleItemsInfo.size

    if (visibleItemsCount >= totalItemsCount) {
        return
    }

    val scrollbarHeightFraction = visibleItemsCount.toFloat() / totalItemsCount
    val scrollbarHeight = size.height * scrollbarHeightFraction

    val firstVisibleItem = visibleItemsInfo.first()
    val firstVisibleItemIndex = firstVisibleItem.index
    val firstVisibleItemOffset = firstVisibleItem.offset

    val scrollbarOffsetYFraction = (firstVisibleItemIndex.toFloat() - firstVisibleItemOffset.toFloat() / firstVisibleItem.size) / totalItemsCount
    val scrollbarOffsetY = size.height * scrollbarOffsetYFraction

    drawRect(
        color = color,
        topLeft = Offset(size.width - widthPx, scrollbarOffsetY),
        size = Size(widthPx, scrollbarHeight)
    )
}
