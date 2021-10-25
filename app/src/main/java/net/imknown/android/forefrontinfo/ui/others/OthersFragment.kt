package net.imknown.android.forefrontinfo.ui.others

import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override val listViewModel by viewModels<OthersViewModel>()

    override fun init() {
        observeLanguageEvent(MyApplication.othersLanguageEvent)
    }
}