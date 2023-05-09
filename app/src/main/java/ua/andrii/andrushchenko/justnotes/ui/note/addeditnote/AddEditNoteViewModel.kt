package ua.andrii.andrushchenko.justnotes.ui.note.addeditnote

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.Constants.EDIT_RESULT_OK
import ua.andrii.andrushchenko.justnotes.utils.DateTimeUtils
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteDao: NoteDao,
    private val reminderHelper: ReminderHelper,
    private val state: SavedStateHandle
) : ViewModel() {

    val note = state.get<Note>("note")

    var noteTitle = state.get<String>("noteTitle") ?: note?.title.orEmpty()
        set(value) {
            field = value
            state.set("noteTitle", value)
        }

    var noteContent = state.get<String>("noteContent") ?: note?.content.orEmpty()
        set(value) {
            field = value
            state.set("noteContent", value)
        }

    var noteIsUrgent = state.get<Boolean>("noteIsUrgent") ?: note?.isUrgent ?: false
        set(value) {
            field = value
            state.set("noteIsUrgent", value)
        }

    private var noteReminderAlarmTimeMillis =
        state.get<Long>("noteReminderAlarmTimeMillis") ?: note?.reminderAlarmTimeMillis ?: -1L

    var noteHasReminder = state.get<Boolean>("noteHasReminder") ?: note?.hasReminder ?: false

    private val addEditNoteEventChannel = Channel<AddEditNoteEvent>()
    val addEditNoteEvent = addEditNoteEventChannel.receiveAsFlow()

    fun onSaveClicked() {
        if (noteContent.isBlank()) {
            sendInvalidInputMessage(R.string.note_content_cannot_be_null)
            return
        }

        if (note != null) {
            val updatedNote = note.copy(
                title = noteTitle,
                content = noteContent,
                isUrgent = noteIsUrgent,
                reminderAlarmTimeMillis = noteReminderAlarmTimeMillis,
                hasReminder = noteHasReminder
            )

            if (noteHasReminder) {
                if (noteReminderAlarmTimeMillis != note.reminderAlarmTimeMillis) {
                    reminderHelper.cancelReminder(note.id)
                    reminderHelper.setReminder(updatedNote)
                }
            } else {
                reminderHelper.cancelReminder(note.id)
            }
            updateNote(updatedNote)

        } else {
            val newNote = Note(
                title = noteTitle,
                content = noteContent,
                isUrgent = noteIsUrgent,
                reminderAlarmTimeMillis = noteReminderAlarmTimeMillis,
                hasReminder = noteHasReminder
            )
            if (noteHasReminder) {
                reminderHelper.setReminder(newNote)
            }
            createNote(newNote)

        }
    }

    private fun createNote(note: Note) = viewModelScope.launch {
        noteDao.insert(note)
        toggleBootCompletedReceiver()
        addEditNoteEventChannel.send(AddEditNoteEvent.NavigateBackWithResult(ADD_RESULT_OK))
    }

    private fun updateNote(note: Note) = viewModelScope.launch {
        noteDao.update(note)
        toggleBootCompletedReceiver()
        addEditNoteEventChannel.send(AddEditNoteEvent.NavigateBackWithResult(EDIT_RESULT_OK))
    }

    private fun sendInvalidInputMessage(@StringRes msg: Int) = viewModelScope.launch {
        addEditNoteEventChannel.send(AddEditNoteEvent.ShowInvalidInputMessage(msg))
    }

    private val _reminderDateTime: MutableStateFlow<ReminderDateTime> = MutableStateFlow(ReminderDateTime())

    fun setDate(
        year: Int,
        month: Int,
        day: Int
    ) {
        _reminderDateTime.update {
            it.copy(
                savedYear = year,
                savedMonth = month,
                savedDay = day
            )
        }
    }

    fun setTime(
        hour: Int,
        minute: Int
    ) {
        _reminderDateTime.update {
            it.copy(
                savedHour = hour,
                savedMinute = minute
            )
        }
    }

    fun setReminderDateTimeByPeriod(reminderPeriod: ReminderHelper.ReminderPeriod) =
        with(Calendar.getInstance()) {
            setDate(
                year = this[Calendar.YEAR],
                month = this[Calendar.MONTH],
                day = this[Calendar.DAY_OF_MONTH]
            )

            val savedHour = when (reminderPeriod) {
                ReminderHelper.ReminderPeriod.MORNING -> 9
                ReminderHelper.ReminderPeriod.AFTERNOON -> 13
                ReminderHelper.ReminderPeriod.EVENING -> 18
                ReminderHelper.ReminderPeriod.LATE_EVENING -> 21
            }

            setTime(
                hour = savedHour,
                minute = 0
            )
        }

    fun saveReminderDateTimeMillis() {
        Calendar.getInstance().apply {
            this[Calendar.YEAR] = _reminderDateTime.value.savedYear
            this[Calendar.MONTH] = _reminderDateTime.value.savedMonth
            this[Calendar.DAY_OF_MONTH] = _reminderDateTime.value.savedDay
            this[Calendar.HOUR] = _reminderDateTime.value.savedHour
            this[Calendar.MINUTE] = _reminderDateTime.value.savedMinute
            this[Calendar.SECOND] = 0
        }.let { calendar ->
            noteReminderAlarmTimeMillis = calendar.timeInMillis
            noteHasReminder = true
            Log.d(
                TAG,
                "saveReminderDateTimeMillis: ${
                    DateTimeUtils.getFormattedString(noteReminderAlarmTimeMillis)
                }"
            )
        }
    }

    fun cancelReminder() {
        noteReminderAlarmTimeMillis = -1L
        noteHasReminder = false
    }

    private fun toggleBootCompletedReceiver() = viewModelScope.launch {
        val notesWithReminders = noteDao.getNotesWithReminders()
        reminderHelper.setBootCompletedBroadcastReceiverState(shouldEnable = notesWithReminders.isNotEmpty())
    }

    sealed interface AddEditNoteEvent {
        data class ShowInvalidInputMessage(@StringRes val msg: Int) : AddEditNoteEvent
        data class NavigateBackWithResult(val result: Int) : AddEditNoteEvent
    }

    companion object {
        private const val TAG = "AddEditNoteViewModel"
    }
}

data class ReminderDateTime(
    val savedYear: Int = 0,
    val savedMonth: Int = 0,
    val savedDay: Int = 0,
    val savedHour: Int = 0,
    val savedMinute: Int = 0
)