package net.imknown.android.forefrontinfo.ui.others

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseFragment

class OthersFragment : BaseFragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_others, container, false)
    }
}