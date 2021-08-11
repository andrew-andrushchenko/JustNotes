package ua.andrii.andrushchenko.justnotes.ui.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ua.andrii.andrushchenko.justnotes.databinding.ItemTodoListBinding
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.ui.base.BaseListAdapter

class TodoListsAdapter(private val listener: OnItemClickListener) : BaseListAdapter<TodoList>(
    TODO_LIST_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemTodoListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoListsViewHolder(binding)
    }

    inner class TodoListsViewHolder(private val binding: ItemTodoListBinding) :
        BaseViewHolder(binding) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val todoList = getItem(position)
                    listener.onItemClick(todoList)
                }
            }
        }

        override fun bind(entity: TodoList) {
            binding.todoListTitle.text = entity.title
        }
    }

    interface OnItemClickListener {
        fun onItemClick(todoList: TodoList)
    }

    companion object {
        private val TODO_LIST_COMPARATOR = object : DiffUtil.ItemCallback<TodoList>() {
            override fun areItemsTheSame(oldItem: TodoList, newItem: TodoList): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TodoList, newItem: TodoList): Boolean =
                oldItem == newItem
        }
    }

}