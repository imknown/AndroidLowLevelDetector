package net.imknown.android.forefrontinfo.ui.others

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.base.BaseListFragment
import net.imknown.android.forefrontinfo.ui.GetRawPropEventViewModel

class OthersFragment : BaseListFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    private val getRawPropEventViewModel by activityViewModels<GetRawPropEventViewModel>()
    override val listViewModel by activityViewModels<OthersViewModel>()

    override fun init() {
        listViewModel.models.observe(viewLifecycleOwner, Observer {
            getRawPropEventViewModel.onFinish()

            showModels(it)
        })

        listViewModel.rawProp.observe(viewLifecycleOwner, Observer {
            launch(Dispatchers.IO) {
                collectModelsCaller(500)
            }
        })
    }
}