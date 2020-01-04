package net.imknown.android.forefrontinfo.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R

class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    private val myModels by lazy { ArrayList<MyModel>() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.fragment_list_item,
            parent,
            false
        )
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.tvTitle.setBackgroundColor(myModels[position].color)
        holder.itemView.tvTitle.text = myModels[position].title
        holder.itemView.tvDetail.text = myModels[position].detail
    }

    override fun getItemCount() = myModels.size

    suspend fun addAll(newModels: ArrayList<MyModel>) = withContext(Dispatchers.Default) {
        myModels.clear()
        myModels.addAll(newModels)
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}