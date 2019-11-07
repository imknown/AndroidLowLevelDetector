package net.imknown.android.forefrontinfo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViews()

        collectionDataset()
    }

    private fun initViews() {
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

        swipeRefresh.isRefreshing = true

        swipeRefresh.setOnRefreshListener {
            collectionDataset()
        }

        list.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(context)

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = MyAdapter(ArrayList())
        }
    }

    protected abstract fun collectionDataset()

    protected fun clear() = getDataset().clear()

    private fun getDataset() = (list.adapter as MyAdapter).myDataset

    protected fun add(result: String) =
        getDataset().add(MyModel(result))

    protected fun add(result: String, condition: Boolean) =
        getDataset().add(MyModel(result, condition))

    protected fun add(result: String, @ColorInt color: Int) =
        getDataset().add(MyModel(result, color))

    protected fun showResult() {
        list.adapter!!.notifyDataSetChanged()

        swipeRefresh.isRefreshing = false
    }
}