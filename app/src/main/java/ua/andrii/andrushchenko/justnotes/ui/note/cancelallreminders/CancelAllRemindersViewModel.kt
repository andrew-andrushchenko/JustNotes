package ua.andrii.andrushchenko.justnotes.ui.note.cancelallreminders

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.di.ApplicationScope
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import javax.inject.Inject

@HiltViewModel
class CancelAllRemindersViewModel @Inject constructor(
    private val noteDao: NoteDao,
    private val reminderHelper: ReminderHelper,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    fun onConfirmClick() = applicationScope.launch {
        val notesWithCancelledReminders = mutableListOf<Note>()
        val notesWithReminders = noteDao.getNotesWithReminders()
        for (note in notesWithReminders) {
            reminderHelper.cancelReminder(note.id)
            notesWithCancelledReminders.add(note.copy(reminderAlarmTimeMillis = -1L, hasReminder = false))
        }
        noteDao.updateAll(notesWithCancelledReminders)
    }
}