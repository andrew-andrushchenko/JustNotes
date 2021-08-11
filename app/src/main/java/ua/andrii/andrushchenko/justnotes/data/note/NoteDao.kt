package ua.andrii.andrushchenko.justnotes.data.note

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.SortOrder

@Dao
interface NoteDao {

    fun getNotes(
        searchQuery: String,
        sortOrder: SortOrder,
        hideNotImportant: Boolean,
        hideWithoutReminders: Boolean
    ): Flow<List<Note>> =
        when (sortOrder) {
            SortOrder.BY_DATE -> getNotesSortedByDate(
                searchQuery,
                hideNotImportant,
                hideWithoutReminders
            )
            SortOrder.BY_NAME -> getNotesSortedByTitle(
                searchQuery,
                hideNotImportant,
                hideWithoutReminders
            )
        }

    @Query("SELECT * FROM notes_table WHERE (hasReminder = :hideWithoutReminders or hasReminder = 1) AND (isUrgent = :hideNotImportant OR isUrgent = 1) AND ((title LIKE '%' || :searchQuery || '%') OR (content LIKE '%' || :searchQuery || '%')) ORDER BY lastEditedTimeMillis ASC")
    fun getNotesSortedByDate(
        searchQuery: String,
        hideNotImportant: Boolean,
        hideWithoutReminders: Boolean
    ): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE (hasReminder = :hideWithoutReminders or hasReminder = 1) AND (isUrgent = :hideNotImportant OR isUrgent = 1) AND ((title LIKE '%' || :searchQuery || '%') OR (content LIKE '%' || :searchQuery || '%')) ORDER BY title ASC")
    fun getNotesSortedByTitle(
        searchQuery: String,
        hideNotImportant: Boolean,
        hideWithoutReminders: Boolean
    ): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE hasReminder = 1")
    suspend fun getNotesWithReminders(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(notes: List<Note>)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes_table")
    suspend fun deleteAll()
}