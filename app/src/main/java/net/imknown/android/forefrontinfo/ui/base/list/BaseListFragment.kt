package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.LiveData
import com.google.android.material.color.MaterialColors
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.mvvm.BaseFragment
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.base.mvvm.toast
import net.imknown.android.forefrontinfo.base.mvvm.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.databinding.BaseListFragmentBinding
import net.imknown.android.forefrontinfo.ui.MainActivity
import net.imknown.android.forefrontinfo.ui.common.setScrollBarMode
import com.google.android.material.R as materialR

abstract class BaseListFragment : BaseFragment<BaseListFragmentBinding>() {

    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> BaseListFragmentBinding =
        BaseListFragmentBinding::inflate

    protected val myAdapter by lazy { MyAdapter() }

    protected abstract val listViewModel: BaseListViewModel

    protected fun observeLanguageEvent(event: LiveData<Event<Unit>>) {
        event.observe(viewLifecycleOwner, EventObserver {
            binding.swipeRefreshLayout.isRefreshing = true

            listViewModel.collectModels()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // super.onViewCreated(view, savedInstanceState)

        initWindowInsets()

        initViews(savedInstanceState)

        // region [ScrollBar Mode]
        listViewModel.scrollBarMode.observe(viewLifecycleOwner, EventObserver {
            binding.recyclerView.setScrollBarMode(it)
        })

        val key = MyApplication.getMyString(R.string.interface_scroll_bar_key)
        val scrollBarMode = MyApplication.sharedPreferences.getString(key, null)
        binding.recyclerView.setScrollBarMode(scrollBarMode)
        // endregion [ScrollBar Mode]

        listViewModel.models.observe(viewLifecycleOwner) {
            listViewModel.showModels(myAdapter.myModels, it)
        }

        listViewModel.showModelsEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyDataSetChanged()

            binding.swipeRefreshLayout.isRefreshing = false
        })

        listViewModel.showErrorEvent.observe(viewLifecycleOwner, EventObserver {
            context?.toast(it)

            binding.swipeRefreshLayout.isRefreshing = false
        })

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