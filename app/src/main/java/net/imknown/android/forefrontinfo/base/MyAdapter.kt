package net.imknown.android.forefrontinfo.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import net.imknown.android.forefrontinfo.R

internal class MyAdapter(internal val myDataset: MutableList<MyModel>) :
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