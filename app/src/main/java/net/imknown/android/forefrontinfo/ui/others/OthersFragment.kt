package net.imknown.android.forefrontinfo.ui.others

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
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository
import net.imknown.android.forefrontinfo.ui.theme.AppTheme

class OthersFragment : Fragment() { // no longer extends BaseListFragment, same as PropFragment (pure copy, only the VM wiring differs)

    companion object {
        fun newInstance() = OthersFragment()
    }

    // This ViewModel wiring is unchanged from the pre-migration code — direct evidence of
    // the "data layer untouched" promise
    private val listViewModel by viewModels<OthersViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = OthersRepository(
                    BasicDataSource(),
                    ArchitectureDataSource(),
                    RomDataSource(),
                    FingerprintDataSource(),
                    KernelDataSource(),
                    OthersDataSource()
                )
                this[OthersViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            OthersViewModel.Factory
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
                MyModelListScreen(listViewModel) // shares the same building block as the Prop page
            }
        }
    }
}
