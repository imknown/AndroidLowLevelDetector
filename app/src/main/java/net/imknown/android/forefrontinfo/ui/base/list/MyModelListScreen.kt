package net.imknown.android.forefrontinfo.ui.base.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.imknown.android.forefrontinfo.R
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

    MyModelListContent(
        models = models,
        isRefreshing = state is State.Loading, // state down: Loading spins (first load included, same as legacy)
        onRefresh = viewModel::refresh, // event up: gesture -> VM refresh (a method reference is just a lambda)
        modifier = modifier,
    )
}

@Composable
private fun MyModelListContent(
    models: PersistentList<MyModel>, // data only (no ViewModel) -> previewable and reusable
    isRefreshing: Boolean, // whether the spinner spins (external state; this composable decides nothing)
    onRefresh: () -> Unit, // refresh callback (event goes up)
    modifier: Modifier = Modifier,
) {
    // Dashboard of the pull gesture: pull distance progress etc. A custom indicator requires
    // owning the state and passing it to both PullToRefreshBox and Indicator
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing, // state in: whether it spins is decided by the parameter, not by us
        onRefresh = onRefresh, // event out: user pulled past the threshold -> notify upstream
        state = pullToRefreshState, // gesture state (must be passed explicitly when the indicator is customized)
        modifier = modifier.fillMaxSize(),
        indicator = { // Indicator slot: defaults to the M3 spinner; customized here to match the legacy colors
            // (legacy: primaryContainer background + onPrimaryContainer spinner).
            // Removing the whole indicator parameter falls back to the M3 default colors
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter), // top-center inside the PullToRefreshBox
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Legacy base_list_fragment.xml: android:background="?attr/colorSurfaceContainer"
                .background(MaterialTheme.colorScheme.surfaceContainer),
            // Top/bottom 12dp is content design (not insets compensation);
            // top bar / bottom bar / system bars are handled by Scaffold innerPadding
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.item_divider_space_vertical)),
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
}

// The screen function takes a ViewModel and cannot be previewed directly;
// the preview targets the data-only MyModelListContent instead
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MyModelListContentPreview() {
    AppTheme {
        MyModelListContent(
            models = previewModels,
            isRefreshing = false,
            onRefresh = {},
        )
    }
}
