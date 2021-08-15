package ua.andrii.andrushchenko.justnotes.ui.note

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.ui.main.ADD_RESULT_OK
import ua.andrii.andrushchenko.justnotes.ui.main.EDIT_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.NotesFilterPreferences
import ua.andrii.andrushchenko.justnotes.utils.PreferencesManager
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NoteDao,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel() {

    val notesSearchQuery = state.getLiveData("notesSearchQuery", "")

    val preferencesFlow = preferencesManager.preferencesFlow.map { preferences ->
        val sortOrder = SortOrder.valueOf(
            preferences[PreferencesManager.PreferencesKeys.NOTES_SORT_ORDER]
                ?: SortOrder.BY_NAME.name
        )
        val hideNotImportant =
            preferences[PreferencesManager.PreferencesKeys.NOTES_HIDE_NOT_IMPORTANT] ?: false
        val hideNoReminders =
            preferences[PreferencesManager.PreferencesKeys.NOTES_HIDE_WITHOUT_REMINDERS] ?: false
        NotesFilterPreferences(sortOrder, hideNotImportant, hideNoReminders)
    }

    private val notesEventChannel = Channel<NoteEvent>()
    val notesEvent = notesEventChannel.receiveAsFlow()

    private val notesFlow: Flow<List<Note>> = combine(
        notesSearchQuery.asFlow(),
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

    val notes: LiveData<List<Note>> = notesFlow.asLiveData()

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
            ADD_RESULT_OK -> showNoteSavedConfirmationMessage("Note added")
            EDIT_RESULT_OK -> showNoteSavedConfirmationMessage("Note updated")
        }
    }

    private fun showNoteSavedConfirmationMessage(text: String) = viewModelScope.launch {
        notesEventChannel.send(NoteEvent.ShowNoteSavedConfirmationMessage(text))
    }

    sealed class NoteEvent {
        object NavigateToAddNoteScreen : NoteEvent()
        data class NavigateToEditNoteScreen(val note: Note) : NoteEvent()
        data class ShowUndoDeleteNoteMessage(val note: Note) : NoteEvent()
        data class ShowNoteSavedConfirmationMessage(val msg: String) : NoteEvent()
        object NavigateToDeleteAllNotes : NoteEvent()
        object NavigateToCancelAllReminders : NoteEvent()
    }

}