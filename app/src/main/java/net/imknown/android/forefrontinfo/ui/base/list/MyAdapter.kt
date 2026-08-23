package net.imknown.android.forefrontinfo.ui.base.list

import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.color.MaterialColors
import net.imknown.android.forefrontinfo.base.MyApplication

class MyAdapter : ListAdapter<MyModel, MyViewHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(parent)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder.binding) {
            val model = getItem(position)

            @AttrRes val color = model.color
            if (color != RES_ID_NONE) {
                val backgroundColor = MaterialColors.getColor(root, color)
                sivColor.setBackgroundColor(backgroundColor)
            } else {
                sivColor.isGone = true
            }

            tvTitle.text = when (val title = model.title) {
                is MyModelTitle.Res -> MyApplication.getMyString(title.id)
                is MyModelTitle.Raw -> title.text
            }
            tvDetail.text = model.detail
        }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<MyModel>() {
            override fun areItemsTheSame(a: MyModel, b: MyModel) = a.key == b.key

            override fun areContentsTheSame(a: MyModel, b: MyModel) = a == b
        }
    }
}
