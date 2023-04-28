package ua.andrii.andrushchenko.justnotes.broadcast

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.andrii.andrushchenko.justnotes.worker.ResetRemindersWorker

class ScheduleExactAlarmPermissionChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                ResetRemindersWorker.enqueueResetReminders(context)
            }
        }
    }
}