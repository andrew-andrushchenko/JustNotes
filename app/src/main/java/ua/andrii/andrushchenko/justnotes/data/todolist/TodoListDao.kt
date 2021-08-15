package ua.andrii.andrushchenko.justnotes.data.todolist

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.utils.SortOrder

@Dao
interface TodoListDao {

    fun getTodoLists(query: String, sortOrder: SortOrder): Flow<List<TodoList>> = when (sortOrder) {
        SortOrder.BY_DATE -> getTodoListsSortedByDate(query)
        SortOrder.BY_NAME -> getTodoListsSortedByName(query)
    }

    @Query("SELECT * FROM todo_lists_table WHERE title LIKE '%' || :searchQuery || '%' ORDER BY created DESC")
    fun getTodoListsSortedByDate(searchQuery: String): Flow<List<TodoList>>

    @Query("SELECT * FROM todo_lists_table WHERE title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun getTodoListsSortedByName(searchQuery: String): Flow<List<TodoList>>

    @Query("SELECT * FROM todo_lists_table WHERE id = :id")
    suspend fun getTodoListById(id: Long): TodoList

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todoList: TodoList): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(todoList: TodoList)

    @Delete
    suspend fun delete(todoList: TodoList)

    @Query("DELETE FROM todo_lists_table")
    suspend fun deleteAll()

}