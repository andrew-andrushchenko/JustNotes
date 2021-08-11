package ua.andrii.andrushchenko.justnotes.domain

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "todo_lists_table")
@Parcelize
data class TodoList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val created: Long = System.currentTimeMillis()
) : Parcelable