package ua.andrii.andrushchenko.justnotes.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.BY_DATE)

    private val notesFlow = combine(
        searchQuery,
        sortOrder
    ) { searchQuery, sortOrder ->
        Pair(searchQuery, sortOrder)
    }.flatMapLatest { (searchQuery, sortOrder) ->
        noteDao.getNotes(searchQuery, sortOrder)
    }

    val notes = notesFlow.asLiveData()

}