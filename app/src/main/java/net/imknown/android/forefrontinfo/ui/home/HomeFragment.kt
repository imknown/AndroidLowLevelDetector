package net.imknown.android.forefrontinfo.ui.home

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.ui.base.GeneralSavedStateViewModelFactory
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.base.list.MyAdapter
import net.imknown.android.forefrontinfo.ui.home.datasource.HomeGatewayDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.HomeLocalDataSource

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    override val listViewModel by viewModels<HomeViewModel> {
        GeneralSavedStateViewModelFactory(
            HomeRepository(HomeLocalDataSource(), HomeGatewayDataSource()),
            this
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.homeLanguageEvent)

        listViewModel.subtitle.observe(viewLifecycleOwner) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.subtitle = MyApplication.getMyString(it.lldDataModeResId, it.dataVersion)
        }

        listViewModel.outdatedOrderProp.observe(viewLifecycleOwner, EventObserver {
            listViewModel.payloadOutdatedTargetSdkVersionApk(myAdapter.myModels)
        })

        listViewModel.showOutdatedOrderEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyItemChanged(myAdapter.myModels.lastIndex, MyAdapter.PAYLOAD_DETAILS)
        })
    }
}
