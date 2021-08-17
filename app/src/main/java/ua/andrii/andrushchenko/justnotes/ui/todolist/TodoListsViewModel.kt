package ua.andrii.andrushchenko.justnotes.ui.todolist

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

    private val todoListsFlow: Flow<List<TodoList>> = combine(
        todoListsSearchQuery.asFlow(),
        preferencesFlow
    ) { query, sortOrder ->
        Pair(query, sortOrder)
    }.flatMapLatest { (query, sortOrder) ->
        todoListDao.getTodoLists(query, sortOrder)
    }

    val todoLists: LiveData<List<TodoList>> = todoListsFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateTodoListsSortOrder(sortOrder)
    }

    fun onTodoListSelected(todoList: TodoList) = viewModelScope.launch {
        todoListsEventChannel.send(TodoListsEvent.NavigateToEditTodoListScreen(todoList))
    }

    fun onAddNewTodoListClicked() = viewModelScope.launch {
        val id = todoListDao.insert(TodoList(title = ""))
        // This is because we need the actual inserted into DB TodoList, which has assigned id
        val newTodoList = todoListDao.getTodoListById(id)
        todoListsEventChannel.send(TodoListsEvent.NavigateToCreateTodoListScreen(newTodoList))
    }

    fun onUndoDeleteClicked(todoList: TodoList, tasks: List<Task>) = viewModelScope.launch {
        todoListDao.insert(todoList)
        tasksDao.insertAll(tasks)
    }

    fun onTodoListSwiped(todoList: TodoList) = viewModelScope.launch {
        todoListDao.delete(todoList)
        val todoListTasks = tasksDao.getTasksBelongTodoList(todoList.id)
        tasksDao.deleteAllTasksBelongTodoList(todoList.id)
        todoListsEventChannel.send(
            TodoListsEvent.ShowUndoDeleteTaskMessage(
                todoList,
                todoListTasks
            )
        )
    }

    fun onDeleteAllClicked() = viewModelScope.launch {
        todoListDao.deleteAll()
        tasksDao.deleteAll()
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_RESULT_OK -> showTodoListSavedConfirmationMessage(R.string.todo_list_added)
            ADD_RESULT_FAIL -> showTodoListSavedConfirmationMessage(R.string.empty_todo_list_discarded)
            EDIT_RESULT_OK -> showTodoListSavedConfirmationMessage(R.string.todo_list_updated)
        }
    }

    private fun showTodoListSavedConfirmationMessage(@StringRes msg: Int) = viewModelScope.launch {
        todoListsEventChannel.send(TodoListsEvent.ShowTodoListSavedConfirmationMessage(msg))
    }

    sealed class TodoListsEvent {
        data class NavigateToCreateTodoListScreen(val todoList: TodoList) : TodoListsEvent()
        data class NavigateToEditTodoListScreen(val todoList: TodoList) : TodoListsEvent()
        data class ShowTodoListSavedConfirmationMessage(@StringRes val msg: Int) : TodoListsEvent()
        data class ShowUndoDeleteTaskMessage(val todoList: TodoList, val tasks: List<Task>) : TodoListsEvent()
    }
}