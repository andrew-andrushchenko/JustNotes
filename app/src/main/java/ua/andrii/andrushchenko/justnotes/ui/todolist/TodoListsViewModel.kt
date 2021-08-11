package ua.andrii.andrushchenko.justnotes.ui.todolist

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
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import javax.inject.Inject

@HiltViewModel
class TodoListsViewModel @Inject constructor(
    private val todoListDao: TodoListDao,
    private val tasksDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel() {

    val todoListsSearchQuery = state.getLiveData("todoListsSearchQuery", "")

    private val preferencesFlow = preferencesManager.preferencesFlow.map { preferences ->
        SortOrder.valueOf(
            preferences[PreferencesManager.PreferencesKeys.TODO_LISTS_SORT_ORDER]
                ?: SortOrder.BY_NAME.name
        )
    }

    private val todoListsEventChannel = Channel<TodoListsEvent>()
    val todoListsEvent = todoListsEventChannel.receiveAsFlow()

    private val todoListsFlow = combine(
        todoListsSearchQuery.asFlow(),
        preferencesFlow
    ) { query, sortOrder ->
        Pair(query, sortOrder)
    }.flatMapLatest { (query, sortOrder) ->
        todoListDao.getTodoLists(query, sortOrder)
    }

    val todoLists = todoListsFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateTodoListsSortOrder(sortOrder)
    }

    fun onTodoListSelected(todoList: TodoList) = viewModelScope.launch {
        todoListsEventChannel.send(TodoListsEvent.NavigateToTasksScreen(todoList))
    }

    fun onAddNewTodoListClicked() = viewModelScope.launch {
        todoListsEventChannel.send(TodoListsEvent.NavigateToCreateTodoListScreen)
    }

    fun onUndoDeleteClicked(todoList: TodoList, tasks: List<Task>) = viewModelScope.launch {
        todoListDao.insert(todoList)
        tasksDao.insertAll(tasks)
    }

    fun onTodoListSwiped(todoList: TodoList) = viewModelScope.launch {
        todoListDao.delete(todoList)
        val todoListTasks = tasksDao.getTasksBelongTodoList(todoList.id)
        tasksDao.deleteAllTasksBelongTodoList(todoList.id)
        todoListsEventChannel.send(TodoListsEvent.ShowUndoDeleteTaskMessage(todoList, todoListTasks))
    }

    fun onDeleteAllClicked() = viewModelScope.launch {
        todoListDao.deleteAll()
        tasksDao.deleteAll()
    }

    fun onAddResult(result: Int) = viewModelScope.launch {
        if (result == ADD_RESULT_OK) {
            todoListsEventChannel.send(TodoListsEvent.ShowTodoListSavedConfirmationMessage("Todo list added"))
        }
    }

    sealed class TodoListsEvent {
        object NavigateToCreateTodoListScreen : TodoListsEvent()
        data class NavigateToTasksScreen(val todoList: TodoList) : TodoListsEvent()
        data class ShowUndoDeleteTaskMessage(val todoList: TodoList, val tasks: List<Task>) : TodoListsEvent()
        data class ShowTodoListSavedConfirmationMessage(val msg: String) : TodoListsEvent()
    }

}