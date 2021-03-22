package net.imknown.android.forefrontinfo.ui.base.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.imknown.android.forefrontinfo.databinding.FragmentListItemBinding

class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
    companion object {
        const val PAYLOAD_DETAILS = 1
    }

    private lateinit var binding: FragmentListItemBinding

    val myModels by lazy { ArrayList<MyModel>() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = FragmentListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                if (it == PAYLOAD_DETAILS) {
                    holder.binding.tvDetail.text = myModels[position].detail
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.tvTitle.setBackgroundResource(myModels[position].color)
        holder.binding.tvTitle.text = myModels[position].title
        holder.binding.tvDetail.text = myModels[position].detail
    }

    override fun getItemCount() = myModels.size
}