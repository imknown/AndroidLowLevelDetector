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
    private var myDataset = ArrayList<MyModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        collectionDataset()

        showResult()
    }

    protected abstract fun collectionDataset()

    private fun showResult() {
        list.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(context)

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = MyAdapter(myDataset)
        }
    }

    protected fun add(result: String) =
        myDataset.add(MyModel(result))

    protected fun add(result: String, condition: Boolean) =
        myDataset.add(MyModel(result, condition))

    protected fun add(result: String, @ColorInt color: Int) =
        myDataset.add(MyModel(result, color))
}