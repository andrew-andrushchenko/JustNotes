package ua.andrii.andrushchenko.justnotes.ui.todolist.addtodolist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.ui.main.ADD_RESULT_OK
import javax.inject.Inject

@HiltViewModel
class AddTodoListViewModel @Inject constructor(
    private val todoListDao: TodoListDao,
    private val state: SavedStateHandle
) : ViewModel() {

    var todoListTitle = state.get<String>("todoListTitle")
        set(value) {
            field = value
            state.set("todoListTitle", value)
        }

    private val addTodoListEventChannel = Channel<AddTodoListEvent>()
    val addTodoListEvent = addTodoListEventChannel.receiveAsFlow()

    fun onSaveClicked() {
        if (todoListTitle.isNullOrBlank()) {
            sendInvalidInputMessage("Title cannot be empty")
            return
        } else {
            val todoList = TodoList(title = todoListTitle!!)
            createTodoList(todoList)
        }
    }

    private fun createTodoList(todoList: TodoList) = viewModelScope.launch {
        todoListDao.insert(todoList)
        addTodoListEventChannel.send(AddTodoListEvent.NavigateBackWithResult(ADD_RESULT_OK))
    }

    @Suppress("SameParameterValue")
    private fun sendInvalidInputMessage(msg: String) = viewModelScope.launch {
        addTodoListEventChannel.send(AddTodoListEvent.ShowInvalidInputMessage(msg))
    }

    sealed class AddTodoListEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddTodoListEvent()
        data class NavigateBackWithResult(val result: Int) : AddTodoListEvent()
    }

}