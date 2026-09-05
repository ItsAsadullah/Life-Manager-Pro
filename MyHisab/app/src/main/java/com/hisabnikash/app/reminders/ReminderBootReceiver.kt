package com.hisabnikash.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hisabnikash.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("ReminderBootReceiver", "Device rebooted, rescheduling active reminders...")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val activeReminders = db.reminderDao().getActiveRemindersSync()
                    val now = System.currentTimeMillis()

                    for (reminder in activeReminders) {
                        if (reminder.triggerTimestamp > now) {
                            ReminderScheduler.scheduleReminder(context, reminder)
                        }
                    }
                    Log.d("ReminderBootReceiver", "Rescheduled ${activeReminders.size} reminders.")
                } catch (e: Exception) {
                    Log.e("ReminderBootReceiver", "Failed to reschedule reminders on boot", e)
                }
            }
        }
    }
}
