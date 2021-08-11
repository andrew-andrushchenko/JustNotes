package ua.andrii.andrushchenko.justnotes.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import ua.andrii.andrushchenko.justnotes.utils.Constants
import ua.andrii.andrushchenko.justnotes.utils.NotificationHelper
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: invoked")
        val id = intent.getIntExtra(Constants.EXTRA_ID, -1)
        val title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(Constants.EXTRA_CONTENT) ?: ""
        //val isUrgent = intent.getBooleanExtra(Constants.EXTRA_IS_URGENT, false)

        notificationHelper.getReminderNotificationBuilder(
            title, content
        ).let { notificationBuilder ->
            notificationHelper.notify(id, notificationBuilder)
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}