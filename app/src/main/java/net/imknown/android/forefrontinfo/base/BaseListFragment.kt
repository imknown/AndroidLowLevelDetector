package net.imknown.android.forefrontinfo.base

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment(), CoroutineScope by MainScope(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var myTempDataset: ArrayList<MyModel>
    private val myAdapter = MyAdapter()

    protected val sharedViewModel: SharedViewModel by activityViewModels()

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

        launch {
            withContext(Dispatchers.IO) {
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
                val scrollBarMode = sharedPreferences.getString(
                    MyApplication.getMyString(R.string.interface_scroll_bar_key),
                    MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
                )!!
                setScrollBarMode(list, scrollBarMode)

                PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
                    .registerOnSharedPreferenceChangeListener(this@BaseListFragment)
            }
        }

        collectionDatasetCaller()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        launch {
            withContext(Dispatchers.IO) {
                if (key == MyApplication.getMyString(R.string.interface_scroll_bar_key)
                    && isActivityAndFragmentOk(this@BaseListFragment)
                ) {
                    val scrollBarMode = sharedPreferences.getString(
                        key,
                        MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
                    )!!

                    withContext(Dispatchers.Main) {
                        setScrollBarMode(list, scrollBarMode)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        myTempDataset.clear()
        myAdapter.addAll(myTempDataset)
        myAdapter.notifyDataSetChanged()

        PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
            .unregisterOnSharedPreferenceChangeListener(this)

        cancel()
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

            layoutManager = LinearLayoutManager(MyApplication.instance)

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = myAdapter
        }
    }

    protected fun collectionDatasetCaller(delay: Long = 0) = launch {
        withContext(Dispatchers.IO) {
            if (delay > 0) {
                delay(delay)
            }

            collectionDataset()
        }
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

    override fun showError(error: Throwable) {
        super.showError(error)

        launch {
            withContext(Dispatchers.Main) {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    protected suspend fun disableSwipeRefresh() = withContext(Dispatchers.Main) {
        swipeRefresh.isEnabled = false
    }

    protected fun sh(cmd: String, condition: Boolean = true): List<String> =
        if (condition) {
            Shell.sh(cmd).exec().out
        } else {
            emptyList()
        }

    @SuppressLint("PrivateApi")
    protected fun getStringProperty(key: String, condition: Boolean = true): String {
        val notFilledString = MyApplication.getMyString(R.string.build_not_filled)

        return if (condition) {
            Class.forName("android.os.SystemProperties").getDeclaredMethod(
                "get",
                String::class.java,
                String::class.java
            ).invoke(null, key, notFilledString) as String
        } else {
            notFilledString
        }
    }
}