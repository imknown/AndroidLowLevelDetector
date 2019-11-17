package net.imknown.android.forefrontinfo.base

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R

abstract class BaseListFragment : BaseFragment(), IView,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var myTempDataset: ArrayList<MyModel>
    private val myAdapter = MyAdapter()

    private lateinit var fastScroller: Any

    override fun getFastScroller(): Any {
        return if (!::fastScroller.isInitialized) {
            createFastScrollerSingletonInstanceAndEnableIt(list)
        } else {
            fastScroller
        }
    }

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

        GlobalScope.launch(Dispatchers.IO) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isFastScrollEnabled = sharedPreferences.getBoolean(
                MyApplication.getMyString(R.string.interface_fast_scroll_key), false
            )
            if (isFastScrollEnabled) {
                fastScroller = createFastScrollerSingletonInstanceAndEnableIt(list)
            }

            PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this@BaseListFragment)
        }

        collectionDatasetCaller()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        if (key == MyApplication.getMyString(R.string.interface_fast_scroll_key)) {
            setFastScrollerStatus(
                list,
                sharedPreferences.getBoolean(key, false).toString().toBoolean()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
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