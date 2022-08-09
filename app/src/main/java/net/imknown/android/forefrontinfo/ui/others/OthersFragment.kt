package net.imknown.android.forefrontinfo.ui.others

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override val listViewModel by viewModels<OthersViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.othersLanguageEvent)
    }
}