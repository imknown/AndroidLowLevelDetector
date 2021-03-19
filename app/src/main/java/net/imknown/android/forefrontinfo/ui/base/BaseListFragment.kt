package net.imknown.android.forefrontinfo.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event
import net.imknown.android.forefrontinfo.base.EventObserver
import net.imknown.android.forefrontinfo.databinding.FragmentListBinding

abstract class BaseListFragment : BaseFragment() {

    private val binding by lazy {
        FragmentListBinding.inflate(layoutInflater, null, false)
    }

    protected val myAdapter by lazy { MyAdapter() }

    protected abstract val listViewModel: BaseListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    protected fun observeLanguageEvent(event: LiveData<Event<Unit>>) {
        event.observe(viewLifecycleOwner, EventObserver {
            binding.swipeRefreshLayout.isRefreshing = true

            listViewModel.collectModels()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        init()

        listViewModel.init(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (binding.recyclerView.adapter != null) {
            binding.recyclerView.adapter = null
        }
    }

    private fun initViews(savedInstanceState: Bundle?) {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorSecondary)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

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

            layoutManager = LinearLayoutManager(MyApplication.instance)

            addItemDecoration(
                MyItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.item_divider_space_horizontal),
                    resources.getDimensionPixelSize(R.dimen.item_divider_space_vertical)
                )
            )

            if (adapter == null) {
                adapter = myAdapter
            }
        }
    }

    abstract fun init()
}