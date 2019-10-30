package net.imknown.android.forefrontinfo.base

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_CRITICAL
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_DEFAULT_STYLE
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_NO_PROBLEM
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

    protected fun add(myModel: MyModel) = myDataset.add(myModel)

    private class MyAdapter(private val myDataset: List<MyModel>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_list_item,
                parent,
                false
            )
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.itemView.card.setCardBackgroundColor(myDataset[position].color)
            holder.itemView.result.text = myDataset[position].result
        }

        override fun getItemCount() = myDataset.size

        internal class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    protected data class MyModel(val result: String, @ColorInt val color: Int) {
        constructor(result: String) : this(result, COLOR_STATE_LIST_DEFAULT_STYLE)

        constructor(result: String, condition: Boolean) : this(
            result, if (condition) {
                COLOR_STATE_LIST_NO_PROBLEM
            } else {
                COLOR_STATE_LIST_CRITICAL
            }
        )
    }

    private class MyItemDecoration(val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }
                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }
}