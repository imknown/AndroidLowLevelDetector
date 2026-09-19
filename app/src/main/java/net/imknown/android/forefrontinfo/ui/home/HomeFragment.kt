package net.imknown.android.forefrontinfo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class HomeFragment : Fragment() { // no longer extends BaseListFragment, same as PropFragment/OthersFragment

    companion object {
        fun newInstance() = HomeFragment()
    }

    // This ViewModel wiring is unchanged from the pre-migration code — direct evidence of
    // the "data layer untouched" promise
    private val listViewModel by viewModels<HomeViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = HomeRepository(
                    LldDataSource(), MountDataSource(), AppInfoDataSource()
                )
                this[HomeViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            HomeViewModel.Factory
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply { // return value swaps "inflated XML" for ComposeView
        // Official recommendation for Fragments: dispose the composition when the
        // ViewTreeLifecycle is destroyed, so no stale composition survives view recreation
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { // Compose-flavored "setContentView"
            AppTheme { // ComposeView inherits no Material theme; wrap explicitly
                HomeScreen(listViewModel) // the event flow is collected in HomeScreen (page-specific component wrapping)
            }
        }
    }
}
