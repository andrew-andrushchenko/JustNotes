package ua.andrii.andrushchenko.justnotes.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.andrii.andrushchenko.justnotes.worker.ResetRemindersWorker

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ResetRemindersWorker.enqueueResetReminders(context)
            }
        }
    }
}