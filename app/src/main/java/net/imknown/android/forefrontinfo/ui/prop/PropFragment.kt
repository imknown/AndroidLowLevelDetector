package net.imknown.android.forefrontinfo.ui.prop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import net.imknown.android.forefrontinfo.ui.base.list.MyModelListScreen
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class PropFragment : Fragment() { // no longer extends BaseListFragment: list assembly fully delegated to Compose

    companion object {
        fun newInstance() = PropFragment()
    }

    // This ViewModel wiring is unchanged from the pre-migration code — direct evidence of
    // the "data layer untouched" promise
    private val listViewModel by viewModels<PropViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = PropRepository(PropertiesDataSource(), SettingsDataSource())
                this[PropViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            PropViewModel.Factory
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
                MyModelListScreen(listViewModel) // hand over to the screen component; Fragment's job ends here
            }
        }
    }
}
