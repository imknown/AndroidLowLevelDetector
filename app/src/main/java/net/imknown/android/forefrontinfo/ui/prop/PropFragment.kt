package net.imknown.android.forefrontinfo.ui.prop

import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment

class PropFragment : BaseListFragment() {

    companion object {
        fun newInstance() = PropFragment()
    }

    override val listViewModel by viewModels<PropViewModel>()

    override fun init() {
        observeLanguageEvent(MyApplication.propLanguageEvent)
    }
}