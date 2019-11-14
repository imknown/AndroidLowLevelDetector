package net.imknown.android.forefrontinfo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment(), IView {

    private lateinit var myTempDataset: ArrayList<MyModel>
    private val myAdapter = MyAdapter()

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

            adapter = myAdapter
        }
    }

    private fun collectionDatasetCaller() = GlobalScope.launch(Dispatchers.IO) {
        collectionDataset()
    }

    protected abstract suspend fun collectionDataset()

    protected fun createNewTempDataset() {
        myTempDataset = ArrayList()
    }

    protected fun add(result: String) =
        myTempDataset.add(MyModel(result))

    protected fun add(result: String, condition: Boolean) =
        myTempDataset.add(MyModel(result, condition))

    protected fun add(result: String, @ColorInt color: Int) =
        myTempDataset.add(MyModel(result, color))

    protected suspend fun showResult() = withContext(Dispatchers.Main) {
        myAdapter.addAll(myTempDataset)
        myAdapter.notifyDataSetChanged()
        myTempDataset.clear()

        swipeRefresh.isRefreshing = false
    }

    protected fun isActivityAndFragmentOk() = with(activity) {
        isAdded && this != null
                && !this.isFinishing
                && !this.isDestroyed
    }

    protected suspend fun disableSwipeRefresh() = withContext(Dispatchers.Main) {
        swipeRefresh.isEnabled = false
    }
}