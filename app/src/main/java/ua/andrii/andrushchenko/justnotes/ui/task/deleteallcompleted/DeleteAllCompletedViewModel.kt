package ua.andrii.andrushchenko.justnotes.ui.task.deleteallcompleted

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.di.ApplicationScope
import javax.inject.Inject

@HiltViewModel
class DeleteAllCompletedViewModel @Inject constructor(
    private val taskDao: TaskDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
    state: SavedStateHandle
) : ViewModel() {

    private val tListId = state.get<Int>("tListId")

    fun onConfirmClick() = applicationScope.launch {
        taskDao.deleteCompletedTasksInTodoList(tListId!!)
    }

}