package net.imknown.android.forefrontinfo.ui.base

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(), IFragmentView {
    override val visualContext by lazy { context }
}