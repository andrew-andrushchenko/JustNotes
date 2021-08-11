package ua.andrii.andrushchenko.justnotes.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import ua.andrii.andrushchenko.justnotes.R

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    // Create notification channels
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannelName = context.getString(R.string.just_notes_reminders_channel_name)
            val remindersChannelDescriptionText = context.getString(R.string.just_notes_reminders_channel_description)
            val remindersResetChannelName = context.getString(R.string.just_notes_reminders_reset_channel_name)
            val remindersResetChannelDescriptionText = context.getString(R.string.just_notes_reminders_reset_channel_description)
            val remindersChannelImportance = NotificationManager.IMPORTANCE_HIGH
            val remindersResetChannelImportance = NotificationManager.IMPORTANCE_LOW

            val remindersChannel = NotificationChannel(
                JUST_NOTES_REMINDERS_CHANNEL_ID,
                remindersChannelName,
                remindersChannelImportance
            ).apply {
                description = remindersChannelDescriptionText
            }

            val remindersResetChannel = NotificationChannel(
                JUST_NOTES_RESET_REMINDERS_CHANNEL_ID,
                remindersResetChannelName,
                remindersResetChannelImportance
            ).apply {
                description = remindersResetChannelDescriptionText
            }

            // Register channel
            with(notificationManager) {
                createNotificationChannel(remindersChannel)
                createNotificationChannel(remindersResetChannel)
            }
        }
    }

    fun getResetReminderNotificationBuilder() =
        NotificationCompat.Builder(context, JUST_NOTES_RESET_REMINDERS_CHANNEL_ID).apply {
            priority = NotificationCompat.PRIORITY_LOW
            setContentTitle("Reminders reset in progress...")
            setContentText("Wait until all reminders will be set again")
            setSmallIcon(android.R.drawable.stat_sys_download)
        }

    fun getReminderNotificationBuilder(
        title: String,
        content: String
    ) = NotificationCompat.Builder(context, JUST_NOTES_REMINDERS_CHANNEL_ID).apply {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph_main)
            .setDestination(R.id.notesFragment)
            .createPendingIntent()

        setContentTitle(title)
        setContentText(content)
        setSmallIcon(R.drawable.ic_note_outlined)
        setAutoCancel(true)
        setDefaults(NotificationCompat.DEFAULT_ALL)
        priority = NotificationCompat.PRIORITY_MAX
        setContentIntent(pendingIntent)
    }


    fun notify(notificationId: Int, notificationBuilder: NotificationCompat.Builder) {
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val JUST_NOTES_REMINDERS_CHANNEL_ID = "just_notes_reminders_channel_id"
        private const val JUST_NOTES_RESET_REMINDERS_CHANNEL_ID = "just_notes_reset_reminders_channel_id"
    }
}