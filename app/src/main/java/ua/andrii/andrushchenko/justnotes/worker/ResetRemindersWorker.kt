package ua.andrii.andrushchenko.justnotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.utils.NotificationHelper
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import java.util.*

@HiltWorker
class ResetRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteDao: NoteDao,
    private val notificationHelper: NotificationHelper,
    private val reminderHelper: ReminderHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val notesWithReminders: List<Note> = noteDao.getNotesWithReminders()

        val resetRemindersNotificationId = id.hashCode()
        val notificationBuilder = notificationHelper.getResetReminderNotificationBuilder()

        setForeground(ForegroundInfo(resetRemindersNotificationId, notificationBuilder.build()))

        for (note in notesWithReminders) {
            reminderHelper.setReminder(note)
        }
        return Result.success()
    }

    companion object {
        fun enqueueResetReminders(context: Context): UUID {
            OneTimeWorkRequestBuilder<ResetRemindersWorker>()
                .build()
                .let { request ->
                    WorkManager.getInstance(context).enqueue(request)
                    return request.id
                }
        }
    }
}