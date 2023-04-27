package ua.andrii.andrushchenko.justnotes.ui.note

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.Constants.EDIT_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.NotesFilterPreferences
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NoteDao,
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow(state.get<String>("notesSearchQuery").orEmpty())
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val preferencesFlow = preferencesManager.preferencesFlow.map { preferences ->
        val sortOrder = SortOrder.valueOf(
            preferences[PreferencesManager.PreferencesKeys.NOTES_SORT_ORDER]
                ?: SortOrder.BY_NAME.name
        )
        val hideNotImportant = preferences[PreferencesManager.PreferencesKeys.NOTES_HIDE_NOT_IMPORTANT] ?: false
        val hideNoReminders = preferences[PreferencesManager.PreferencesKeys.NOTES_HIDE_WITHOUT_REMINDERS] ?: false
        NotesFilterPreferences(sortOrder, hideNotImportant, hideNoReminders)
    }

    private val notesEventChannel = Channel<NoteEvent>()
    val notesEvent = notesEventChannel.receiveAsFlow()

    private val notesFlow: Flow<List<Note>> = combine(
        _searchQuery,
        preferencesFlow
    ) { searchQuery, notesFilterPreferences ->
        Pair(searchQuery, notesFilterPreferences)
    }.flatMapLatest { (searchQuery, notesFilterPreferences) ->
        noteDao.getNotes(
            searchQuery,
            notesFilterPreferences.sortOrder,
            notesFilterPreferences.hideNotImportant,
            notesFilterPreferences.hideWithoutReminders
        )
    }

    val notes: StateFlow<List<Note>> = notesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateNotesSortOrder(sortOrder)
    }

    fun onHideNotImportantClicked(hideNotImportant: Boolean) = viewModelScope.launch {
        preferencesManager.updateNotesHideNotImportant(hideNotImportant)
    }

    fun onHideNotesWithoutReminders(hideNotesWithoutReminders: Boolean) = viewModelScope.launch {
        preferencesManager.updateNotesHideWithoutReminders(hideNotesWithoutReminders)
    }

    fun onAddNewNoteClicked() = viewModelScope.launch {
        notesEventChannel.send(NoteEvent.NavigateToAddNoteScreen)
    }

    fun onNoteSelected(note: Note) = viewModelScope.launch {
        notesEventChannel.send(NoteEvent.NavigateToEditNoteScreen(note))
    }

    fun onNoteSwiped(note: Note) = viewModelScope.launch {
        noteDao.delete(note)
        notesEventChannel.send(NoteEvent.ShowUndoDeleteNoteMessage(note))
    }

    fun onUndoDeleteClicked(note: Note) = viewModelScope.launch {
        noteDao.insert(note)
    }

    fun onCancelAllRemindersClicked() = viewModelScope.launch {
        notesEventChannel.send(NoteEvent.NavigateToCancelAllReminders)
    }

    fun onDeleteAllNotesClicked() = viewModelScope.launch {
        noteDao.deleteAll()
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_RESULT_OK -> showNoteSavedConfirmationMessage(R.string.note_added)
            EDIT_RESULT_OK -> showNoteSavedConfirmationMessage(R.string.note_updated)
        }
    }

    fun onQueryTextChanged(query: String) {
        _searchQuery.update { query }
        state["notesSearchQuery"] = query
    }

    private fun showNoteSavedConfirmationMessage(@StringRes msg: Int) = viewModelScope.launch {
        notesEventChannel.send(NoteEvent.ShowNoteSavedConfirmationMessage(msg))
    }

    sealed class NoteEvent {
        object NavigateToAddNoteScreen : NoteEvent()
        data class NavigateToEditNoteScreen(val note: Note) : NoteEvent()
        data class ShowUndoDeleteNoteMessage(val note: Note) : NoteEvent()
        data class ShowNoteSavedConfirmationMessage(@StringRes val msg: Int) : NoteEvent()
        object NavigateToDeleteAllNotes : NoteEvent()
        object NavigateToCancelAllReminders : NoteEvent()
    }
}