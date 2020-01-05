package net.imknown.android.forefrontinfo.ui.others

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment
import net.imknown.android.forefrontinfo.ui.GetRawPropEventViewModel
import net.imknown.android.forefrontinfo.ui.settings.booleanLiveData

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

        val key = MyApplication.getMyString(R.string.function_raw_build_prop_key)
        val defValue = false
        MyApplication.sharedPreferences.booleanLiveData(key, defValue)
            .observe(viewLifecycleOwner, Observer {
                launch(Dispatchers.IO) {
                    collectModelsCaller(500)
                }
            })
    }
}