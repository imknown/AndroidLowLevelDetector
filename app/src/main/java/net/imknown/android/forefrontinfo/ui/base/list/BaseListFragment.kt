package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.databinding.BaseListFragmentBinding
import net.imknown.android.forefrontinfo.ui.MainActivity
import net.imknown.android.forefrontinfo.ui.base.BaseFragment
import net.imknown.android.forefrontinfo.ui.base.ext.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.common.setScrollBarMode
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel
import com.google.android.material.R as materialR

abstract class BaseListFragment : BaseFragment<BaseListFragmentBinding>() {

    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> BaseListFragmentBinding =
        BaseListFragmentBinding::inflate

    protected val myAdapter by lazy { MyAdapter() }

    protected abstract val listViewModel: BaseListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initWindowInsets()

        initViews()

        // region [ScrollBar Mode]
        val scrollBarModeKey = MyApplication.getMyString(R.string.interface_scroll_bar_key)
        val scrollBarMode = MyApplication.sharedPreferences.getString(scrollBarModeKey, null)
        binding.recyclerView.setScrollBarMode(scrollBarMode)

        viewLifecycleOwner.lifecycleScope.launch {
            SettingsViewModel.scrollBarModeChangedSharedFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                binding.recyclerView.setScrollBarMode(it)
            }
        }
        // endregion [ScrollBar Mode]

        viewLifecycleOwner.lifecycleScope.launch {
            listViewModel.modelsStateFlow.flowWithLifecycle(
                viewLifecycleOwner.lifecycle
            ).collect { stateMyModels ->
                when (stateMyModels) {
                    State.NotInitialized -> return@collect
                    State.Loading -> binding.swipeRefreshLayout.isRefreshing = true
                    is State.Done -> {
                        myAdapter.submitList(stateMyModels.value)

                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            listViewModel.init(savedInstanceState)
        }
    }

    private fun initWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { rv, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
            (activity as? MainActivity)?.binding?.bottomNavigationView?.doOnLayout { bnv ->
                rv.updatePadding(
                    left = insets.left,
                    right = insets.right,
                    bottom = bnv.height
                )
            }

            windowInsetsCompat
        }
    }

    private fun initViews() {
        val color = MaterialColors.getColor(binding.root, materialR.attr.colorOnPrimaryContainer)
        binding.swipeRefreshLayout.setColorSchemeColors(color)
        val backgroundColor =
            MaterialColors.getColor(binding.root, materialR.attr.colorPrimaryContainer)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(backgroundColor)

        binding.swipeRefreshLayout.setOnRefreshListener {
            listViewModel.refresh()
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)

            addItemDecoration(
                MyItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.item_divider_space_horizontal),
                    resources.getDimensionPixelSize(R.dimen.item_divider_space_vertical)
                )
            )

            adapter = myAdapter
        }
    }
}