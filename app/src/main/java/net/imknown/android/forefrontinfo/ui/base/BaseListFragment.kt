package net.imknown.android.forefrontinfo.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.Event
import net.imknown.android.forefrontinfo.base.EventObserver

abstract class BaseListFragment : BaseFragment() {

    protected val myAdapter by lazy { MyAdapter() }

    protected abstract val listViewModel: BaseListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    protected fun observeLanguageEvent(event: LiveData<Event<Int>>) {
        event.observe(viewLifecycleOwner, EventObserver {
            swipeRefreshLayout.isRefreshing = true

            listViewModel.collectModels()
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViews(savedInstanceState)

        listViewModel.changeScrollBarModeEvent.observe(viewLifecycleOwner, EventObserver {
            recyclerView.isVerticalScrollBarEnabled = it
        })

        listViewModel.models.observe(viewLifecycleOwner, Observer {
            listViewModel.showModels(myAdapter.myModels, it)
        })

        listViewModel.showModelsEvent.observe(viewLifecycleOwner, EventObserver {
            myAdapter.notifyDataSetChanged()

            swipeRefreshLayout.isRefreshing = false
        })

        listViewModel.showErrorEvent.observe(viewLifecycleOwner, EventObserver {
            toast(it)

            swipeRefreshLayout.isRefreshing = false
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

        if (recyclerView.adapter != null) {
            recyclerView.adapter = null
        }
    }

    private fun initViews(savedInstanceState: Bundle?) {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

        if (listViewModel.hasNoData(savedInstanceState)) {
            // When activity is recreated, data is filled by memory.
            // It is fast. No progress indicator needed indeed.
            swipeRefreshLayout.isRefreshing = true
        }

        swipeRefreshLayout.setOnRefreshListener {
            listViewModel.collectModels()
        }

        recyclerView.apply {
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