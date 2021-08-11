package ua.andrii.andrushchenko.justnotes.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import java.io.IOException

private const val TAG = "PreferencesManager"

enum class SortOrder { BY_NAME, BY_DATE }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class TasksFilterPreferences(val sortOrder: SortOrder, val hideCompleted: Boolean)

data class NotesFilterPreferences(
    val sortOrder: SortOrder,
    val hideNotImportant: Boolean,
    val hideWithoutReminders: Boolean
)

class PreferencesManager(context: Context) {

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

    suspend fun updateNotesHideNotImportant(hideNotImportant: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTES_HIDE_NOT_IMPORTANT] = hideNotImportant
        }
    }

    suspend fun updateNotesHideWithoutReminders(hideWithoutReminders: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTES_HIDE_WITHOUT_REMINDERS] = hideWithoutReminders
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
        val NOTES_HIDE_NOT_IMPORTANT = booleanPreferencesKey("notes_hide_not_important")
        val NOTES_HIDE_WITHOUT_REMINDERS = booleanPreferencesKey("notes_hide_without_reminders")
        val TODO_LISTS_SORT_ORDER = stringPreferencesKey("todo_lists_sort_order")
        val TASKS_SORT_ORDER = stringPreferencesKey("tasks_sort_order")
        val TASKS_HIDE_COMPLETED = booleanPreferencesKey("tasks_hide_completed")
    }
}