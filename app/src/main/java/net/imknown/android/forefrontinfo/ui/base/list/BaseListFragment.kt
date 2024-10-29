package net.imknown.android.forefrontinfo.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.LiveData
import com.google.android.material.color.MaterialColors
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.mvvm.BaseFragment
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.databinding.FragmentListBinding
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

    private fun initViews(savedInstanceState: Bundle?) {
        val bottomR = materialR.dimen.design_bottom_navigation_height
        val bottom = resources.getDimensionPixelSize(bottomR)
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom + (insets.bottom + bottom)
            )

            windowInsets
        }

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