package net.imknown.android.forefrontinfo.base

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list_item.view.*
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

        showResult(collectionDataset())
    }

    protected abstract fun collectionDataset(): ArrayList<MyModel>

    private fun showResult(myDataset: ArrayList<MyModel>) {
        list.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(context)

            itemAnimator = DefaultItemAnimator()

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = MyAdapter(myDataset)
        }
    }

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
            holder.itemView.result.setBackgroundResource(myDataset[position].color)
            holder.itemView.result.text = myDataset[position].result
        }

        override fun getItemCount() = myDataset.size

        internal class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    protected data class MyModel(val result: String, @ColorRes val color: Int) {
        companion object {
            private const val ID_DEFAULT_STYLE = 0
        }

        constructor(result: String) : this(result, ID_DEFAULT_STYLE)
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