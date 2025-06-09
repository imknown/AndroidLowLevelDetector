package net.imknown.android.forefrontinfo.ui.base.list

import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
    companion object {
        const val PAYLOAD_DETAILS = 1
    }

    val myModels by lazy { mutableListOf<MyModel>() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(parent)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindFullViewHolder(holder, position)
        } else {
            onBindPayloadViewHolder(holder, position, payloads)
        }
    }

    /** Do **NOT** use this function. Use [onBindViewHolder] above instead */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {}

    private fun onBindFullViewHolder(holder: MyViewHolder, position: Int) {
        with(holder.binding) {
            @AttrRes val color = myModels[position].color
            if (color != RES_ID_NONE) {
                val backgroundColor = MaterialColors.getColor(root, color)
                sivColor.setBackgroundColor(backgroundColor)
            } else {
                sivColor.isGone = true
            }
            tvTitle.text = myModels[position].title
            tvDetail.text = myModels[position].detail
        }
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