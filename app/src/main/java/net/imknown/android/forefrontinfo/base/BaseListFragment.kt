package net.imknown.android.forefrontinfo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.*
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

        collectionDatasetCaller()
    }

    private fun initViews() {
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

        swipeRefresh.isRefreshing = true

        swipeRefresh.setOnRefreshListener {
            collectionDatasetCaller()
        }

        list.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(context)

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = MyAdapter(ArrayList())
        }
    }

    private fun collectionDatasetCaller() {
        GlobalScope.launch(Dispatchers.IO) {
            collectionDataset()
        }
    }

    protected abstract suspend fun collectionDataset()

    protected fun clear() = getDataset().clear()

    private fun getDataset() = (list.adapter as MyAdapter).myDataset

    protected fun add(result: String) =
        getDataset().add(MyModel(result))

    protected fun add(result: String, condition: Boolean) =
        getDataset().add(MyModel(result, condition))

    protected fun add(result: String, @ColorInt color: Int) =
        getDataset().add(MyModel(result, color))

    protected suspend fun showResult() = withContext(Dispatchers.Main) {
        list.adapter!!.notifyDataSetChanged()

        delay(550)

        swipeRefresh.isRefreshing = false
    }

    protected suspend fun disableSwipeRefresh() = withContext(Dispatchers.Main) {
        swipeRefresh.isEnabled = false
    }
}