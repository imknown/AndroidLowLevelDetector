package net.imknown.android.forefrontinfo.ui.base.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
    companion object {
        const val PAYLOAD_DETAILS = 1
    }

    val myModels by lazy { ArrayList<MyModel>() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(parent)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            onBindFullViewHolder(holder, position)
        } else {
            onBindPayloadViewHolder(holder, position, payloads)
        }
    }

    /** Do **NOT** use this function. Use [onBindViewHolder] above instead */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {}

    private fun onBindFullViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.tvTitle.setBackgroundResource(myModels[position].color)
        holder.binding.tvTitle.text = myModels[position].title
        holder.binding.tvDetail.text = myModels[position].detail
    }

    private fun onBindPayloadViewHolder(
        holder: MyViewHolder, position: Int, payloads: MutableList<Any>
    ) = payloads.forEach {
        if (it == PAYLOAD_DETAILS) {
            holder.binding.tvDetail.text = myModels[position].detail
        }
    }

    override fun getItemCount() = myModels.size
}