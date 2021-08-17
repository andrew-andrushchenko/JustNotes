package ua.andrii.andrushchenko.justnotes.ui.todolist.addedittodolist

import androidx.annotation.StringRes
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_RESULT_FAIL
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.Constants.EDIT_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.TasksFilterPreferences
import javax.inject.Inject

@HiltViewModel
class AddEditTodoListViewModel @Inject constructor(
    private val todoListDao: TodoListDao,
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel() {

    val todoList = state.get<TodoList>("todoList")

    var newTodoListTitle: String = todoList?.title ?: ""

    val tasksSearchQuery = state.getLiveData("tasksSearchQuery", "")

    val preferencesFlow = preferencesManager.preferencesFlow.map { preferences ->
        val sortOrder = SortOrder.valueOf(
            preferences[PreferencesManager.PreferencesKeys.TASKS_SORT_ORDER]
                ?: SortOrder.BY_DATE.name
        )
        val hideCompleted =
            preferences[PreferencesManager.PreferencesKeys.TASKS_HIDE_COMPLETED] ?: false
        TasksFilterPreferences(sortOrder, hideCompleted)
    }

    private val tasksEventChannel = Channel<AddEditTodoListEvent>()
    val tasksEvent = tasksEventChannel.receiveAsFlow()

    private val tasksFlow: Flow<List<Task>> = combine(
        tasksSearchQuery.asFlow(),
        preferencesFlow
    ) { query, tasksFilterPreferences ->
        Pair(query, tasksFilterPreferences)
    }.flatMapLatest { (query, tasksFilterPreferences) ->
        todoList?.let {
            taskDao.getTasks(
                query,
                it.id,
                tasksFilterPreferences.sortOrder,
                tasksFilterPreferences.hideCompleted
            )
        } ?: emptyFlow()
    }

    val tasks: LiveData<List<Task>> = tasksFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateTasksSortOrder(sortOrder)
    }

    fun onHideCompletedClicked(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateTasksHideCompleted(hideCompleted)
    }

    fun onTaskSelected(task: Task) = viewModelScope.launch {
        tasksEventChannel.send(AddEditTodoListEvent.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        tasksEventChannel.send(AddEditTodoListEvent.ShowUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClicked(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClicked() = viewModelScope.launch {
        tasksEventChannel.send(AddEditTodoListEvent.NavigateToAddTaskScreen)
    }

    fun saveTodoListAndNavigateBack() = viewModelScope.launch {
        todoList?.let {
            val todoListTasks = taskDao.getTasksBelongTodoList(it.id)
            if (newTodoListTitle.isBlank() && todoListTasks.isEmpty()) {
                todoListDao.delete(it)
                tasksEventChannel.send(AddEditTodoListEvent.NavigateBackWithResult(ADD_RESULT_FAIL))
            } else {
                todoListDao.update(it.copy(title = newTodoListTitle))
                val isTodoListNew = it.title.isBlank()
                tasksEventChannel.send(
                    AddEditTodoListEvent.NavigateBackWithResult(
                        if (isTodoListNew) ADD_RESULT_OK else EDIT_RESULT_OK
                    )
                )
            }
        }
    }

    fun onDeleteAllCompletedInTodoListClicked() = viewModelScope.launch {
        tasksEventChannel.send(AddEditTodoListEvent.NavigateToDeleteAllCompletedInTodoListScreen)
    }

    fun onAddEditTaskResult(result: Int) {
        when (result) {
            ADD_RESULT_OK -> showTaskSavedConfirmationMessage(R.string.task_added)
            EDIT_RESULT_OK -> showTaskSavedConfirmationMessage(R.string.task_updated)
        }
    }

    private fun showTaskSavedConfirmationMessage(@StringRes msg: Int) = viewModelScope.launch {
        tasksEventChannel.send(AddEditTodoListEvent.ShowTaskSavedConfirmationMessage(msg))
    }

    sealed class AddEditTodoListEvent {
        object NavigateToAddTaskScreen : AddEditTodoListEvent()
        object NavigateToDeleteAllCompletedInTodoListScreen : AddEditTodoListEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTodoListEvent()
        data class NavigateToEditTaskScreen(val task: Task) : AddEditTodoListEvent()
        data class ShowUndoDeleteTaskMessage(val task: Task) : AddEditTodoListEvent()
        data class ShowTaskSavedConfirmationMessage(@StringRes val msg: Int) : AddEditTodoListEvent()
    }
}