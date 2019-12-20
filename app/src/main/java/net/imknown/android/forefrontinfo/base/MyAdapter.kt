package net.imknown.android.forefrontinfo.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import net.imknown.android.forefrontinfo.R

class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    private val myDataset = ArrayList<MyModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.fragment_list_item,
            parent,
            false
        )
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.tvTitle.setBackgroundColor(myDataset[position].color)
        holder.itemView.tvTitle.text = myDataset[position].title
        holder.itemView.tvDetail.text = myDataset[position].detail
    }

    override fun getItemCount() = myDataset.size

    fun addAll(newDataset: ArrayList<MyModel>) {
        myDataset.clear()
        myDataset.addAll(newDataset)
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}