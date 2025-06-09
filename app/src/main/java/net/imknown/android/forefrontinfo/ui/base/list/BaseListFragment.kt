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
import net.imknown.android.forefrontinfo.ui.base.ext.toast
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

        initViews(savedInstanceState)

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
            listViewModel.modelsStateFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { stateMyModels ->
                if (stateMyModels == State.NotInitialized) {
                    return@collect
                }

                listViewModel.showModels(myAdapter.myModels, stateMyModels.toValue())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            listViewModel.showModelsEventStateFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { state ->
                if (state == State.NotInitialized) {
                    return@collect
                }

                myAdapter.notifyDataSetChanged()

                binding.swipeRefreshLayout.isRefreshing = false

                listViewModel.clearShowModelsEventStateFlow()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            listViewModel.showErrorEventStateFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { stateMessage ->
                if (stateMessage == State.NotInitialized) {
                    return@collect
                }

                context?.toast(stateMessage.toValue())

                binding.swipeRefreshLayout.isRefreshing = false

                listViewModel.clearShowErrorEventStateFlow()
            }
        }

        listViewModel.init(savedInstanceState)
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

    private fun initViews(savedInstanceState: Bundle?) {
        val color = MaterialColors.getColor(binding.root, materialR.attr.colorOnPrimaryContainer)
        binding.swipeRefreshLayout.setColorSchemeColors(color)
        val backgroundColor =
            MaterialColors.getColor(binding.root, materialR.attr.colorPrimaryContainer)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(backgroundColor)

        if (listViewModel.hasNoData(savedInstanceState)) {
            // When activity is recreated, data is filled by memory.
            // It is fast. No progress indicator needed indeed.
            binding.swipeRefreshLayout.isRefreshing = true
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            listViewModel.collectModels()
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