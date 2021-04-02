package net.imknown.android.forefrontinfo.ui.others

import androidx.fragment.app.activityViewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override val listViewModel by activityViewModels<OthersViewModel>()

    override fun init() {
        observeLanguageEvent(MyApplication.othersLanguageEvent)
    }
}