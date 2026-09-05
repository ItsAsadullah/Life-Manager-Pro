package com.hisabnikash.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hisabnikash.app.data.local.ReminderEntity

object ReminderScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.hisabnikash.app.ACTION_TRIGGER_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.hisabnikash.app.ACTION_SNOOZE_REMINDER"
    const val ACTION_COMPLETE_REMINDER = "com.hisabnikash.app.ACTION_COMPLETE_REMINDER"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTE = "extra_note"
    const val EXTRA_ALERT_TYPE = "extra_alert_type"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_EVENT_TIME = "extra_event_time"

    /**
     * AlarmManager-এ একটি রিমাইন্ডার শিডিউল করে
     */
    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (!reminder.isEnabled || reminder.isCompleted) {
            cancelReminder(context, reminder.id)
            return
        }

        val triggerAtMillis = reminder.triggerTimestamp
        val now = System.currentTimeMillis()

        // যদি সময় পার হয়ে গিয়ে থাকে তবে শিডিউল করার প্রয়োজন নেই
        if (triggerAtMillis <= now) {
            Log.d("ReminderScheduler", "Trigger time is in the past: $triggerAtMillis <= $now")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_NOTE, reminder.note)
            putExtra(EXTRA_ALERT_TYPE, reminder.alertType)
            putExtra(EXTRA_CATEGORY, reminder.category)
            putExtra(EXTRA_EVENT_TIME, reminder.eventTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("ReminderScheduler", "Successfully scheduled reminder: ${reminder.title} at $triggerAtMillis")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error scheduling exact alarm", e)
        }
    }

    /**
     * ১০ মিনিট পর স্নুজ করার জন্য শিডিউল করে
     */
    fun snoozeReminder(context: Context, reminderId: String, title: String, alertType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_NOTE, "১০ মিনিট স্নুজকৃত রিমাইন্ডার")
            putExtra(EXTRA_ALERT_TYPE, alertType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error snoozing alarm", e)
        }
    }

    /**
     * একটি শিডিউলকৃত রিমাইন্ডার বাতিল করে
     */
    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled reminder: $reminderId")
        }
    }
}
