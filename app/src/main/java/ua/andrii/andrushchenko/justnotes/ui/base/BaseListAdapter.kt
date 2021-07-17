package ua.andrii.andrushchenko.justnotes.ui.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseListAdapter<Entity : Any>(diffUtil: DiffUtil.ItemCallback<Entity>) :
    ListAdapter<Entity, BaseListAdapter<Entity>.BaseViewHolder>(diffUtil) {

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) holder.bind(currentItem)
    }

    abstract inner class BaseViewHolder(binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(entity: Entity)
    }
}