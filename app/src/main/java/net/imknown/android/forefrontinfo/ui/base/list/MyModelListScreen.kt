package net.imknown.android.forefrontinfo.ui.base.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.MainActivity
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

/**
 * Shared screen for the three list pages (Home/Others/Prop).
 * Knows only [BaseListViewModel], never concrete pages.
 */
@Composable
fun MyModelListScreen(
    viewModel: BaseListViewModel, // screen-level composable taking a ViewModel is the officially endorsed layer
    modifier: Modifier = Modifier,
) {
    // 1) Subscribe: Flow -> Compose state; collection pauses automatically while STOPPED
    val state by viewModel.modelsStateFlow.collectAsStateWithLifecycle()

    // 2) Side effect: mirrors the listViewModel.init() call at the end of legacy BaseListFragment.
    //    Never call it directly in the composable body (recomposition would re-trigger it);
    //    init() is idempotent, so re-running it on view recreation is harmless.
    LaunchedEffect(viewModel) { viewModel.init() }

    // 3) Derived state: on refresh, state flips back to Loading. Keep the most recent Done data so
    //    the list stays on screen while the spinner spins (legacy behavior: no flash of empty list).
    val models by produceState(
        initialValue = persistentListOf(), // empty at first (the list is empty during the very first load anyway)
        key1 = state, // the block below re-runs every time state changes
    ) {
        // Only Done writes; Loading/NotInitialized do nothing -> the previous value is preserved
        (state as? State.Done<List<MyModel>>)?.let { value = it.value.toPersistentList() }
    }

    MyModelListContent(models, modifier) // state ready, hand over to the pure presentation part
}

@Composable
private fun MyModelListContent(
    models: PersistentList<MyModel>, // data only (no ViewModel) -> previewable and reusable
    modifier: Modifier = Modifier,
) {
    // Transitional insets handling (replaced by Scaffold in step 6):
    // horizontal = system bars (legacy updatePadding(left/right = insets));
    // bottom = bottom navigation bar height, measured off the host Activity as the legacy code did.
    // displayCutout is unioned to mirror the legacy windowInsetsCompatTypes (systemBars or displayCutout)
    val horizontal = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Horizontal) // sides only: the top is already covered by the app bar
        .asPaddingValues() // insets -> PaddingValues usable as contentPadding
    val bottomBarHeight = rememberBottomBarHeight()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            // Legacy base_list_fragment.xml: android:background="?attr/colorSurfaceContainer"
            .background(MaterialTheme.colorScheme.surfaceContainer),
        // Mirrors MyItemDecoration (12dp around, 12dp above the first item) + clipToPadding=false:
        // contentPadding scrolls with the content (legacy clipToPadding=false), it does not shrink the viewport
        contentPadding = PaddingValues(
            start = horizontal.calculateStartPadding(LocalLayoutDirection.current),
            top = dimensionResource(R.dimen.item_divider_space_vertical),
            end = horizontal.calculateEndPadding(LocalLayoutDirection.current),
            bottom = bottomBarHeight + dimensionResource(R.dimen.item_divider_space_vertical),
        ),
        // Fixed spacing between items (legacy ItemDecoration bottom = spaceV)
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.item_divider_space_vertical)
        ),
    ) {
        items(
            items = models,
            // Same key as legacy DiffUtil.areItemsTheSame; without it animations and scroll state break
            key = { it.key },
            // Same hint as legacy RecyclerView viewType
            contentType = { it.type },
        ) { model ->
            MyModelCard(
                model,
                // Legacy MyItemDecoration left/right = spaceH
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.item_divider_space_horizontal)
                ),
            )
        }
    }
}

/**
 * Transitional bridge: the bottom bar is still a View BottomNavigationView, invisible to
 * Compose, so measure it off the host Activity, same trick as the legacy code.
 */
@Composable
private fun rememberBottomBarHeight(): Dp {
    val density = LocalDensity.current
    val bottomBar = (LocalView.current.context as? MainActivity)?.binding?.bottomNavigationView
    val heightPx = remember { mutableIntStateOf(0) }

    if (bottomBar != null) {
        // doOnLayout: the height is only valid after the first layout pass
        LaunchedEffect(bottomBar) {
            bottomBar.doOnLayout { heightPx.intValue = it.height }
        }
    }

    return with(density) { heightPx.intValue.toDp() }
}

// The screen function takes a ViewModel and cannot be previewed directly;
// the preview targets the data-only MyModelListContent instead
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MyModelListContentPreview() {
    AppTheme {
        MyModelListContent(previewModels)
    }
}
