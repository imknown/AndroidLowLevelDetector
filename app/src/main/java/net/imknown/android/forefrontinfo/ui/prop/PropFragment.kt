package net.imknown.android.forefrontinfo.ui.prop

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment

class PropFragment : BaseListFragment() {

    companion object {
        fun newInstance() = PropFragment()
    }

    override val listViewModel by viewModels<PropViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.propLanguageEvent)
    }
}