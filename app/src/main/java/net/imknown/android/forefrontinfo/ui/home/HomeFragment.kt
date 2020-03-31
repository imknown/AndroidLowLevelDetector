package net.imknown.android.forefrontinfo.ui.home

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.base.EventObserver
import net.imknown.android.forefrontinfo.ui.base.BaseListFragment
import net.imknown.android.forefrontinfo.ui.base.MyAdapter

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    override val listViewModel by activityViewModels<HomeViewModel>()

    override fun init() {
        observeLanguageEvent(MyApplication.homeLanguageEvent)

        listViewModel.subtitle.observe(viewLifecycleOwner, Observer {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.subtitle = MyApplication.getMyString(it.lldDataModeResId, it.dataVersion)
        })

        listViewModel.error.observe(viewLifecycleOwner, EventObserver {
            listViewModel.showError(it)
        })

        listViewModel.outdatedOrderProp.observe(viewLifecycleOwner, EventObserver {
            listViewModel.payloadOutdatedTargetSdkVersionApk(myAdapter.myModels)
        })

        listViewModel.showOutdatedOrderEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyItemChanged(myAdapter.myModels.lastIndex, MyAdapter.PAYLOAD_DETAILS)
        })
    }
}
