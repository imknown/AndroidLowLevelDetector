package net.imknown.android.forefrontinfo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment(), CoroutineScope by MainScope() {

    private val myAdapter = MyAdapter()

    protected abstract val listViewModel: BaseListViewModel

    private class MyLongLifeCycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        fun startListening() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun stopListening() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override fun getLifecycle() = lifecycleRegistry
    }

    private val myLongLifeCycleOwner = MyLongLifeCycleOwner()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViews(savedInstanceState)

        myLongLifeCycleOwner.startListening()

        listViewModel.language.observe(myLongLifeCycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                listViewModel.models.value?.clear()
            }
        })

        listViewModel.scrollBarMode.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { scrollBarMode ->
                launch(Dispatchers.IO) {
                    setScrollBarMode(recyclerView, scrollBarMode)
                }
            }
        })

        init()

        launch(Dispatchers.IO) {
            val scrollBarMode = MyApplication.sharedPreferences.getString(
                MyApplication.getMyString(R.string.interface_scroll_bar_key),
                MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
            )!!
            setScrollBarMode(recyclerView, scrollBarMode)

            // When activity is recreated, use LiveData to restore the data
            if (hasNoData(savedInstanceState)) {
                listViewModel.collectModels()
            }
        }
    }

    private fun hasNoData(savedInstanceState: Bundle?) =
        savedInstanceState == null || listViewModel.models.value.isNullOrEmpty()

    override fun onDestroyView() {
        super.onDestroyView()

        myLongLifeCycleOwner.stopListening()

        cancel()
    }

    private fun initViews(savedInstanceState: Bundle?) {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

        if (hasNoData(savedInstanceState)) {
            // When activity is recreated, data is filled by memory.
            // It is fast. No progress indicator needed indeed.
            swipeRefreshLayout.isRefreshing = true
        }

        swipeRefreshLayout.setOnRefreshListener {
            launch(Dispatchers.IO) {
                listViewModel.collectModels()
            }
        }

        recyclerView.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(MyApplication.instance)

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = myAdapter
        }
    }

    abstract fun init()

    protected suspend fun collectModelsCaller(delay: Long) {
        delay(delay)

        listViewModel.collectModels()
    }

    protected fun showModels(myModels: ArrayList<MyModel>) = launch(Dispatchers.Default) {
        if (myModels.isNullOrEmpty()) {
            return@launch
        }

        myAdapter.addAll(myModels)

        withContext(Dispatchers.Main) {
            myAdapter.notifyDataSetChanged()

            swipeRefreshLayout.isRefreshing = false
        }
    }

    protected fun showError(error: Exception) = launch {
        toast(error.message.toString())

        swipeRefreshLayout.isRefreshing = false

        withContext(Dispatchers.Default) {
            if (BuildConfig.DEBUG) {
                error.printStackTrace()
            }
        }
    }

//    protected suspend fun disableSwipeRefresh() = withContext(Dispatchers.Main) {
//        swipeRefresh.isEnabled = false
//    }
}