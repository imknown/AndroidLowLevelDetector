package net.imknown.android.forefrontinfo.base

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment(), CoroutineScope by MainScope(),
    SharedPreferences.OnSharedPreferenceChangeListener {

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

        init()

        launch(Dispatchers.IO) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
            val scrollBarMode = sharedPreferences.getString(
                MyApplication.getMyString(R.string.interface_scroll_bar_key),
                MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
            )!!
            setScrollBarMode(recyclerView, scrollBarMode)

            PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
                .registerOnSharedPreferenceChangeListener(this@BaseListFragment)

            collectModels()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        launch(Dispatchers.IO) {
            if (key == MyApplication.getMyString(R.string.interface_scroll_bar_key)) {
                val scrollBarMode = sharedPreferences.getString(
                    key,
                    MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
                )!!

                setScrollBarMode(recyclerView, scrollBarMode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
            .unregisterOnSharedPreferenceChangeListener(this)

        cancel()
    }

    private fun initViews() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorStateless)

        swipeRefreshLayout.isRefreshing = true

        swipeRefreshLayout.setOnRefreshListener {
            launch(Dispatchers.IO) {
                collectModels()
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

        collectModels()
    }

    protected abstract suspend fun collectModels()

    protected fun showModels(newModels: java.util.ArrayList<MyModel>) {
        launch {
            myAdapter.addAll(newModels)

            withContext(Dispatchers.Main) {
                myAdapter.notifyDataSetChanged()

                swipeRefreshLayout.isRefreshing = false
            }
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