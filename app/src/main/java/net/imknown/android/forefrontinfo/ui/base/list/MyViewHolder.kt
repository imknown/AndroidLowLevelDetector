package net.imknown.android.forefrontinfo.ui.base.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.imknown.android.forefrontinfo.base.mvvm.viewBinding
import net.imknown.android.forefrontinfo.databinding.FragmentListItemBinding

class MyViewHolder(
    parent: ViewGroup,
    val binding: FragmentListItemBinding = parent.viewBinding(FragmentListItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root)