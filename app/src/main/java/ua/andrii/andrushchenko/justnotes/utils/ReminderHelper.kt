package ua.andrii.andrushchenko.justnotes.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import ua.andrii.andrushchenko.justnotes.broadcast.BootCompletedReceiver
import ua.andrii.andrushchenko.justnotes.broadcast.ReminderReceiver
import ua.andrii.andrushchenko.justnotes.domain.Note

class ReminderHelper(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun setReminder(note: Note) {
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(Constants.EXTRA_ID, note.id)
            putExtra(Constants.EXTRA_TITLE, note.title)
            putExtra(Constants.EXTRA_CONTENT, note.content)
        }.let { intent ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                note.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when {
                    alarmManager.canScheduleExactAlarms() -> {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            note.reminderAlarmTimeMillis,
                            pendingIntent
                        )
                    }
                    else -> {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    note.reminderAlarmTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "setReminder: set successful")
        }
    }

    fun cancelReminder(reminderId: Int) {
        Intent(context, ReminderReceiver::class.java).let { intent ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "cancelReminder: cancel successful")
        }
    }

    fun setBootCompletedBroadcastReceiverState(shouldEnable: Boolean) {
        val receiver = ComponentName(context, BootCompletedReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            if (shouldEnable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d(TAG, "setBootCompletedBroadcastReceiverState: enabled: $shouldEnable")
    }

    enum class ReminderPeriod {
        MORNING, AFTERNOON, EVENING, LATE_EVENING
    }

    companion object {
        private const val TAG = "ReminderHelper"
    }
}