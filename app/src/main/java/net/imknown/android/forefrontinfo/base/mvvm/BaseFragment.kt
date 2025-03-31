package net.imknown.android.forefrontinfo.base.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T : ViewBinding> : Fragment(), FragmentMixin {
    override val visualContext by lazy { context }

    private var _binding: T? = null
    protected val binding by lazy { _binding!! }

    protected abstract val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = viewBinding(inflate, container)
        return _binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}