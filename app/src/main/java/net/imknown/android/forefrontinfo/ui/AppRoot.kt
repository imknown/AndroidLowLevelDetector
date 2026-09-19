package net.imknown.android.forefrontinfo.ui

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.persistentListOf
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.list.MyModelListScreen
import net.imknown.android.forefrontinfo.ui.home.HomeScreen
import net.imknown.android.forefrontinfo.ui.home.HomeViewModel
import net.imknown.android.forefrontinfo.ui.navigation.HomeKey
import net.imknown.android.forefrontinfo.ui.navigation.OthersKey
import net.imknown.android.forefrontinfo.ui.navigation.PropKey
import net.imknown.android.forefrontinfo.ui.navigation.SettingsKey
import net.imknown.android.forefrontinfo.ui.others.OthersViewModel
import net.imknown.android.forefrontinfo.ui.prop.PropViewModel
import net.imknown.android.forefrontinfo.ui.settings.SettingsScreen
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel

private data class TopLevelTab(
    val key: NavKey, // navigation key (unique identity)
    @DrawableRes val iconRes: Int, // icon (reuses the legacy menu vector resources)
    @StringRes val labelRes: Int, // label (reuses the legacy menu strings)
)

// Bottom navigation data source: the legacy bottom_nav_menu.xml becomes a plain list
private val topLevelTabs = persistentListOf(
    TopLevelTab(HomeKey, R.drawable.ic_home_24dp, R.string.title_home),
    TopLevelTab(OthersKey, R.drawable.ic_others_24dp, R.string.title_others),
    TopLevelTab(PropKey, R.drawable.ic_prop_24dp, R.string.title_prop),
    TopLevelTab(SettingsKey, R.drawable.ic_settings_24dp, R.string.title_settings),
)

/**
 * Single-Activity + Navigation 3 multi-back-stack skeleton: each top-level tab owns an
 * independent back stack (= one browsing history per browser tab; switching tabs swaps the
 * rendered stack). Unrendered stacks keep their UI state and ViewModels alive — the same
 * behavior as the legacy Fragment show/hide.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still an experimental API in m3 1.4.0
@Composable
fun AppRoot() {
    // Grab the Activity in composition: the onBack lambda is not a composable context and
    // cannot read it inside
    val activity = LocalActivity.current

    // Mirrors MainViewModel.lastId ("remember the last tab"): rememberSaveable survives
    // rotation and process death; storing the index is the simplest option (fixed key list)
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // One back stack per tab; map is inline, so @Composable rememberNavBackStack can be
    // called inside; the stack content is rememberSaveable-backed -> survives process death
    val backStacks = topLevelTabs.associate { tab -> tab.key to rememberNavBackStack(tab.key) }

    // Entry decorators: "plugins" that attach capabilities to each navigation entry —
    // 1) freeze/unfreeze the entry's rememberSaveable state (scroll positions etc.)
    // 2) give the entry its own ViewModelStore (the source of entry-scoped ViewModels)
    val entryDecorators: List<NavEntryDecorator<NavKey>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )

    // Turn the keys in each stack into decorated, renderable entries — one pass per tab
    val decoratedEntries = topLevelTabs.map { tab ->
        rememberDecoratedNavEntries(
            backStack = backStacks.getValue(tab.key), // this tab's own stack
            entryDecorators = entryDecorators, // decorator list
            entryProvider = entryProvider { // key -> UI mapping DSL
                entry<HomeKey> { HomeScreen(viewModel(factory = HomeViewModel.Factory)) }
                entry<OthersKey> { MyModelListScreen(viewModel<OthersViewModel>(factory = OthersViewModel.Factory)) } // explicit type: the factory only registers the concrete class (asking for the abstract BaseListViewModel crashes)
                entry<PropKey> { MyModelListScreen(viewModel<PropViewModel>(factory = PropViewModel.Factory)) } // explicit type, same as above
                entry<SettingsKey> { SettingsScreen(viewModel(factory = SettingsViewModel.Factory)) }
            }, // viewModel() called inside the entry content -> automatically scoped to that entry (the VM decorator)
        )
    }

    Scaffold( // page scaffold: top bar / bottom bar / content three sections
        topBar = {
            // mirrors the default title bar after the legacy setSupportActionBar(toolbar) (Activity label)
            // legacy AppBarLayout: android:background="?attr/colorSurfaceContainer" — same slot
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        bottomBar = {
            NavigationBar { // = the M3 counterpart of the legacy BottomNavigationView
                topLevelTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == currentTabIndex, // selected state is driven by state
                        onClick = { currentTabIndex = index }, // click only changes state, rendering follows
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding -> // avoidance amounts computed by Scaffold (top + bottom bars + system bars) — end of the manual insets era
        NavDisplay(
            entries = decoratedEntries[currentTabIndex], // render only the entries of the currently selected tab stack
            onBack = { activity?.finish() }, // keep legacy behavior: back from any tab exits directly
            modifier = Modifier.padding(innerPadding), // consume the scaffold-provided insets in one line
        )
    }
}
