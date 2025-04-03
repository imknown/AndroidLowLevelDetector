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
import net.imknown.android.forefrontinfo.base.mvvm.BaseFragment
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.base.mvvm.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.databinding.FragmentListBinding
import net.imknown.android.forefrontinfo.ui.MainActivity
import com.google.android.material.R as materialR

abstract class BaseListFragment : BaseFragment<FragmentListBinding>() {

    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> FragmentListBinding =
        FragmentListBinding::inflate

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

        listViewModel.changeScrollBarModeEvent.observe(viewLifecycleOwner, EventObserver {
            binding.recyclerView.isVerticalScrollBarEnabled = it
        })

        listViewModel.models.observe(viewLifecycleOwner) {
            listViewModel.showModels(myAdapter.myModels, it)
        }

        listViewModel.showModelsEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyDataSetChanged()

            binding.swipeRefreshLayout.isRefreshing = false
        })

        listViewModel.showErrorEvent.observe(viewLifecycleOwner, EventObserver {
            toast(it)

            binding.swipeRefreshLayout.isRefreshing = false
        })

        listViewModel.scrollBarMode.observe(viewLifecycleOwner, EventObserver {
            it?.let { scrollBarMode ->
                listViewModel.setScrollBarMode(scrollBarMode)
            }
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