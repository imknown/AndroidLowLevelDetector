package net.imknown.android.forefrontinfo.ui.others

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override val listViewModel by viewModels<OthersViewModel>(
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.othersLanguageEvent)
    }
}