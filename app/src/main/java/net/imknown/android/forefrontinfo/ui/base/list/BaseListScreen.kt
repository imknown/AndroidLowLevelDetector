package net.imknown.android.forefrontinfo.ui.base.list

import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel

@Composable
fun BaseListScreen(
    viewModel: BaseListViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current

    val stateMyModels = viewModel.modelsState
    val loading = viewModel.loading

    var displayModels by remember { mutableStateOf<List<MyModel>>(emptyList()) }
    
    if (stateMyModels is State.Done) {
        displayModels = stateMyModels.value
    }

    LaunchedEffect(viewModel) {
        if (viewModel.hasNoData()) {
            viewModel.init()
        }
    }

    val sharedPreferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val scrollBarKey = stringResource(id = R.string.interface_scroll_bar_key)
    val scrollBarDefaultValue = stringResource(id = R.string.interface_no_scroll_bar_value)
    val scrollBarModeSetting = remember(sharedPreferences, SettingsViewModel.scrollBarModeChanged) {
        sharedPreferences.getString(scrollBarKey, scrollBarDefaultValue)
    }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = {
            viewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScrollbar(listState, scrollBarModeSetting),
            contentPadding = PaddingValues(
                horizontal = dimensionResource(id = R.dimen.item_divider_space_horizontal),
                vertical = dimensionResource(id = R.dimen.item_divider_space_vertical)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.item_divider_space_vertical))
        ) {
            items(displayModels, key = { it.title + it.detail }) { model ->
                val circleColor = remember(model.color, context) {
                    if (model.color == RES_ID_NONE) {
                        Color.Transparent
                    } else {
                        val typedValue = TypedValue()
                        if (context.theme.resolveAttribute(model.color, typedValue, true)) {
                            Color(typedValue.data)
                        } else {
                            Color.Transparent
                        }
                    }
                }

                MyViewHolderItem(
                    title = model.title,
                    detail = model.detail,
                    circleColor = circleColor,
                    onClick = { /* Handle click if needed */ }
                )
            }
        }
    }
}
