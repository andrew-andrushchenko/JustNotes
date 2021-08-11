package ua.andrii.andrushchenko.justnotes.ui.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.ItemNoteBinding
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.ui.base.BaseListAdapter
import ua.andrii.andrushchenko.justnotes.utils.DateTimeUtils

class NotesAdapter(private val listener: OnItemClickListener) : BaseListAdapter<Note>(
    NOTES_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesViewHolder(binding)
    }

    inner class NotesViewHolder(private val binding: ItemNoteBinding) : BaseViewHolder(binding) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val note = getItem(position)
                    listener.onItemClick(note)
                }
            }
        }

        override fun bind(entity: Note) {
            with(binding) {
                txtTitle.text = entity.title
                txtContent.text = entity.content
                root.strokeColor = if (entity.isUrgent)
                    root.resources.getColor(R.color.red_400, null)
                else
                    root.resources.getColor(R.color.green_300, null)
                if (entity.reminderAlarmTimeMillis != -1L) {
                    txtReminderTime.text = DateTimeUtils.getFormattedString(entity.reminderAlarmTimeMillis)
                    txtReminderTime.visibility = View.VISIBLE
                } else {
                    txtReminderTime.visibility = View.GONE
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(note: Note)
    }

    companion object {
        private val NOTES_COMPARATOR = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem == newItem
        }
    }

}