package ua.andrii.andrushchenko.justnotes.data.note

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.SortOrder

@Dao
interface NoteDao {

    fun getNotes(searchQuery: String, sortOrder: SortOrder): Flow<List<Note>> =
        when (sortOrder) {
            SortOrder.BY_DATE -> getNotesSortedByDate(searchQuery)
            SortOrder.BY_NAME -> getNotesSortedByTitle(searchQuery)
        }

    @Query("SELECT * FROM notes_table WHERE (title LIKE '%' || :searchQuery || '%') OR (content LIKE '%' || :searchQuery || '%') ORDER BY lastEditedTimeMillis DESC")
    fun getNotesSortedByDate(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE (title LIKE '%' || :searchQuery || '%') OR (content LIKE '%' || :searchQuery || '%') ORDER BY title DESC")
    fun getNotesSortedByTitle(searchQuery: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes_table")
    suspend fun deleteAll()
}