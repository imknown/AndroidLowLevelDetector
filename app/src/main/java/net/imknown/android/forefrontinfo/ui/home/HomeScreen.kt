package net.imknown.android.forefrontinfo.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import net.imknown.android.forefrontinfo.ui.base.list.MyModelListScreen
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel

/**
 * Home-specific screen: wraps the shared list screen to collect this page's cross-page
 * event; the shared screen stays ignorant. Composition units are functions, so wrapping
 * costs nothing at runtime.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    // Mirrors the SharedFlow collection in the legacy onViewCreated: LaunchedEffect starts on
    // entering composition and cancels on leaving. Note it does not pause on lifecycle STOPPED
    // (unlike the legacy flowWithLifecycle): this event is only emitted by user interaction on
    // the Settings page and never in background, so the difference is moot. If a future flow
    // may emit in background, wrap with repeatOnLifecycle(STARTED) to match legacy behavior.
    LaunchedEffect(viewModel) {
        SettingsViewModel.outdatedOrderChangedSharedFlow.collect { // the Settings page "broadcast"
            viewModel.payloadOutdatedTargetSdkVersionApk() // refresh the matching entry on receipt
        }
    }

    MyModelListScreen(viewModel, modifier) // everything else reuses the shared list screen (HomeViewModel is a BaseListViewModel subclass)
}
