package ua.andrii.andrushchenko.justnotes.data.task

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.utils.SortOrder

@Dao
interface TaskDao {

    fun getTasks(
        query: String,
        todoListId: Int,
        sortOrder: SortOrder,
        hideCompleted: Boolean
    ): Flow<List<Task>> =
        when (sortOrder) {
            SortOrder.BY_DATE -> getTasksSortedByDateCreated(query, todoListId, hideCompleted)
            SortOrder.BY_NAME -> getTasksSortedByName(query, todoListId, hideCompleted)
        }

    @Query("SELECT * FROM tasks_table WHERE todoListId = :todoListId AND (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY important DESC, name")
    fun getTasksSortedByName(
        searchQuery: String,
        todoListId: Int,
        hideCompleted: Boolean
    ): Flow<List<Task>>

    @Query("SELECT * FROM tasks_table WHERE todoListId = :todoListId AND (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY important DESC, created")
    fun getTasksSortedByDateCreated(
        searchQuery: String,
        todoListId: Int,
        hideCompleted: Boolean
    ): Flow<List<Task>>

    @Query("SELECT * FROM tasks_table WHERE todoListId = :todoListId")
    suspend fun getTasksBelongTodoList(todoListId: Int): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks_table WHERE todoListId = :todoListId AND completed = 1")
    suspend fun deleteCompletedTasksInTodoList(todoListId: Int)

    @Query("DELETE FROM tasks_table WHERE todoListId = :todoListId")
    suspend fun deleteAllTasksBelongTodoList(todoListId: Int)

    @Query("DELETE FROM tasks_table")
    suspend fun deleteAll()

}