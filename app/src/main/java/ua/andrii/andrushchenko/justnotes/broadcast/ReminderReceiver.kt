package ua.andrii.andrushchenko.justnotes.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import ua.andrii.andrushchenko.justnotes.utils.Constants
import ua.andrii.andrushchenko.justnotes.utils.NotificationHelper
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Constants.EXTRA_ID, -1)
        val title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(Constants.EXTRA_CONTENT) ?: ""

        notificationHelper.getReminderNotificationBuilder(
            title, content
        ).let { notificationBuilder ->
            notificationHelper.notify(id, notificationBuilder)
        }
    }
}