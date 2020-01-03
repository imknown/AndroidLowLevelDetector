package net.imknown.android.forefrontinfo.ui.home

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.base.BaseListFragment

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private val homeViewModel = HomeViewModel()

    override fun init() {
        homeViewModel.subtitle.observe(viewLifecycleOwner, Observer {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.subtitle = MyApplication.getMyString(it.lldDataModeResId, it.dataVersion)
        })

        homeViewModel.models.observe(viewLifecycleOwner, Observer {
            showModels(it)
        })

        homeViewModel.error.observe(viewLifecycleOwner, Observer {
            showError(it)
        })
    }

    override suspend fun collectModels() {
        homeViewModel.collectModels()
    }
}
