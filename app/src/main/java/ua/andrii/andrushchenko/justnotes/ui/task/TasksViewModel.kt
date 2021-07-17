package ua.andrii.andrushchenko.justnotes.ui.task

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.ui.main.ADD_RESULT_OK
import ua.andrii.andrushchenko.justnotes.ui.main.EDIT_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.TasksFilterPreferences
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val todoListDao: TodoListDao,
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel() {

    val todoList = state.get<TodoList>("todoList")

    val tasksSearchQuery = state.getLiveData("tasksSearchQuery", "")

    val preferencesFlow = preferencesManager.preferencesFlow.map { preferences ->
        val sortOrder = SortOrder.valueOf(
            preferences[PreferencesManager.PreferencesKeys.TASKS_SORT_ORDER]
                ?: SortOrder.BY_DATE.name
        )
        val hideCompleted = preferences[PreferencesManager.PreferencesKeys.TASKS_HIDE_COMPLETED] ?: false
        TasksFilterPreferences(sortOrder, hideCompleted)
    }

    private val tasksEventChannel = Channel<TasksEvent>()
    val tasksEvent = tasksEventChannel.receiveAsFlow()

    private val tasksFlow = combine(
        tasksSearchQuery.asFlow(),
        preferencesFlow
    ) { query, tasksFilterPreferences ->
        Pair(query, tasksFilterPreferences)
    }.flatMapLatest { (query, filterPreferences) ->
        taskDao.getTasks(query, todoList!!.id, filterPreferences.sortOrder, filterPreferences.hideCompleted)
    }

    val tasks = tasksFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateTasksSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateTasksHideCompleted(hideCompleted)
    }

    fun onTaskSelected(task: Task) = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClicked(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClicked() = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_RESULT_OK -> showTaskSavedConfirmationMessage("Task added")
            EDIT_RESULT_OK -> showTaskSavedConfirmationMessage("Task updated")
        }
    }

    private fun showTaskSavedConfirmationMessage(text: String) = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun onDeleteAllCompletedInTodoListClicked() = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToDeleteAllCompletedInTodoListScreen)
    }

    fun onChangeTodoListTitleClicked(title: String) = viewModelScope.launch {
        val todoList = todoList?.copy(title = title)
        todoListDao.update(todoList!!)
    }

    sealed class TasksEvent {
        object NavigateToAddTaskScreen : TasksEvent()
        data class NavigateToEditTaskScreen(val task: Task) : TasksEvent()
        data class ShowUndoDeleteTaskMessage(val task: Task) : TasksEvent()
        data class ShowTaskSavedConfirmationMessage(val msg: String) : TasksEvent()
        object NavigateToDeleteAllCompletedInTodoListScreen : TasksEvent()
    }
}