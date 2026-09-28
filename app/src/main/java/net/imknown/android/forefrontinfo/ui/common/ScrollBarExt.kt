package net.imknown.android.forefrontinfo.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Sized and timed to the platform scrollbar the View-era app got from isVerticalScrollBarEnabled:
// 4dp thick (config_scrollbarSize), flush with the trailing edge (scrollbarStyle insideOverlay),
// and the framework fade values (scrollbarFadeDuration / scrollbarDefaultDelayBeforeFade)
private val ScrollBarThickness = 4.dp

private const val ScrollBarFadeMillis = 250
private const val ScrollBarFadeOutDelayMillis = 300

/**
 * Vertical scroll indicator for a scrolling list: the thumb appears while the list scrolls and fades
 * out once it settles. Nothing is draggable -- it only shows where the viewport sits, which is what
 * the View-era "normal" scroll bar mode came down to (the retired "fast / draggable" option never got
 * an implementation).
 *
 * Stands in for the official `Modifier.nonInteractiveScrollbar` (androidx.compose.material3), which the
 * pinned Material3 does not ship yet. When it lands: delete this file, and at each call site pass
 * `scrollState.scrollIndicatorState` instead of the scroll state, turning the `enabled` argument into
 * the condition that guards the call.
 */
@Composable
fun Modifier.nonInteractiveScrollbar(
    scrollState: ScrollableState,
    enabled: Boolean = true,
): Modifier {
    val indicatorState = scrollState.scrollIndicatorState ?: return this
    val scrolling = enabled && scrollState.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if (scrolling) 1f else 0f,
        animationSpec = tween(
            // The thumb appears with no animation: View sets the scrollbar alpha straight to 255 when
            // scrolling starts and only interpolates the fade-out (scrollbarFadeDuration + delay)
            durationMillis = if (scrolling) 0 else ScrollBarFadeMillis,
            delayMillis = if (scrolling) 0 else ScrollBarFadeOutDelayMillis,
        ),
        label = "scrollbarAlpha",
    )
    // The platform thumb is #84ffffff tinted by colorControlNormal, and Material3 maps that attr to
    // colorOnSurfaceVariant -- the same color at the same 52% alpha
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)

    return drawWithContent {
        drawContent()
        if (alpha > 0f) {
            val viewportSize = indicatorState.viewportSize
            val contentSize = indicatorState.contentSize
            // Int.MAX_VALUE means "not measured yet"; content fitting inside the viewport has nothing to indicate.
            // A lazy layout reports both extents estimated (average visible item size x item count), which is
            // all a non-interactive thumb needs
            if (viewportSize > 0 && contentSize != Int.MAX_VALUE && contentSize > viewportSize) {
                val thickness = ScrollBarThickness.toPx()
                val trackHeight = size.height
                // The platform's own thumb math (ScrollBarUtils.getThumbLength / getThumbOffset):
                // viewport-to-content ratio, floored at twice the thickness so a very long list
                // does not shrink the thumb towards nothing
                val barHeight = (trackHeight * viewportSize / contentSize)
                    .coerceAtLeast(thickness * 2)
                val trackRange = trackHeight - barHeight
                val barTop = (trackRange * indicatorState.scrollOffset / (contentSize - viewportSize))
                    .coerceIn(0f, trackRange)
                val barLeft = if (layoutDirection == LayoutDirection.Rtl) {
                    0f
                } else {
                    size.width - thickness
                }
                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(thickness, barHeight),
                    alpha = alpha,
                )
            }
        }
    }
}
