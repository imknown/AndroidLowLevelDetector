package net.imknown.android.forefrontinfo.ui.home

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.base.list.MyAdapter

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    override val listViewModel by activityViewModels<HomeViewModel>()

    override fun init() {
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
