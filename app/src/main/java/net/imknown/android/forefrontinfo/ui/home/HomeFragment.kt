package net.imknown.android.forefrontinfo.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.EventObserver
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.base.list.MyAdapter
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    override val listViewModel by viewModels<HomeViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = HomeRepository(
                    LldDataSource(), AndroidDataSource(), MountDataSource(), AppInfoDataSource()
                )
                this[HomeViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            HomeViewModel.Factory
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.homeLanguageEvent)

        listViewModel.outdatedOrderProp.observe(viewLifecycleOwner, EventObserver {
            listViewModel.payloadOutdatedTargetSdkVersionApk(myAdapter.myModels)
        })

        listViewModel.showOutdatedOrderEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyItemChanged(myAdapter.myModels.lastIndex, MyAdapter.PAYLOAD_DETAILS)
        })
    }
}
