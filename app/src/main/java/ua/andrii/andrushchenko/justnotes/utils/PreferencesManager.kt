package ua.andrii.andrushchenko.justnotes.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PreferencesManager"

enum class SortOrder { BY_NAME, BY_DATE }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class TasksFilterPreferences(val sortOrder: SortOrder, val hideCompleted: Boolean)

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    suspend fun updateNotesSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTES_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateTodoListsSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TODO_LISTS_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateTasksSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASKS_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateTasksHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASKS_HIDE_COMPLETED] = hideCompleted
        }
    }

    object PreferencesKeys {
        val NOTES_SORT_ORDER = stringPreferencesKey("notes_sort_order")
        val TODO_LISTS_SORT_ORDER = stringPreferencesKey("todo_lists_sort_order")
        val TASKS_SORT_ORDER = stringPreferencesKey("tasks_sort_order")
        val TASKS_HIDE_COMPLETED = booleanPreferencesKey("tasks_hide_completed")
    }
}