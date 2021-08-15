package ua.andrii.andrushchenko.justnotes.ui.todolist.addedittodolist

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import ua.andrii.andrushchenko.justnotes.di.ApplicationScope
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.TasksFilterPreferences
import javax.inject.Inject

@HiltViewModel
class AddEditTodoListViewModel @Inject constructor(
    private val todoListDao: TodoListDao,
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
    state: SavedStateHandle
) : ViewModel() {

    val todoList = state.get<TodoList>("todoList")

    var todoListTitle: String = todoList?.title ?: ""

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

    /** Application scope is using here to let DB operations be finalized
    * after current view model becomes cleared.
    * This method should be called when user navigates back */
    fun onSaveTodoList() = applicationScope.launch {
        todoList?.let {
            val todoListTasks = taskDao.getTasksBelongTodoList(it.id)
            if (todoListTitle.isBlank() && todoListTasks.isEmpty()) {
                todoListDao.delete(it)
            } else {
                todoListDao.update(it.copy(title = todoListTitle))
            }
        }
    }

    fun onDeleteAllCompletedInTodoListClicked() = viewModelScope.launch {
        tasksEventChannel.send(AddEditTodoListEvent.NavigateToDeleteAllCompletedInTodoListScreen)
    }

    sealed class AddEditTodoListEvent {
        object NavigateToAddTaskScreen : AddEditTodoListEvent()
        object NavigateToDeleteAllCompletedInTodoListScreen : AddEditTodoListEvent()
        data class NavigateToEditTaskScreen(val task: Task) : AddEditTodoListEvent()
        data class ShowUndoDeleteTaskMessage(val task: Task) : AddEditTodoListEvent()
        data class ShowTaskSavedConfirmationMessage(val msg: String) : AddEditTodoListEvent()
    }
}