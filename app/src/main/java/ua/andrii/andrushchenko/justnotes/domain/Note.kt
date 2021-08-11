package ua.andrii.andrushchenko.justnotes.domain

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "notes_table")
@Parcelize
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isUrgent: Boolean = false,
    val reminderAlarmTimeMillis: Long = -1L,
    val hasReminder: Boolean = reminderAlarmTimeMillis != -1L,
    val lastEditedTimeMillis: Long = System.currentTimeMillis()
) : Parcelable