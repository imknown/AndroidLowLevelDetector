package net.imknown.android.forefrontinfo.ui.base.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.imknown.android.forefrontinfo.databinding.MyViewHolderBinding
import net.imknown.android.forefrontinfo.ui.base.ext.viewBinding

class MyViewHolder(
    parent: ViewGroup,
    val binding: MyViewHolderBinding = parent.viewBinding(MyViewHolderBinding::inflate)
) : RecyclerView.ViewHolder(binding.root)