package net.imknown.android.forefrontinfo.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import net.imknown.android.forefrontinfo.R

class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    companion object {
        const val PAYLOAD_DETAILS = 1
    }

    val myModels by lazy { ArrayList<MyModel>() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.fragment_list_item,
            parent,
            false
        )
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                if (it == PAYLOAD_DETAILS) {
                    holder.itemView.tvDetail.text = myModels[position].detail
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.tvTitle.setBackgroundResource(myModels[position].color)
        holder.itemView.tvTitle.text = myModels[position].title
        holder.itemView.tvDetail.text = myModels[position].detail
    }

    override fun getItemCount() = myModels.size

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}