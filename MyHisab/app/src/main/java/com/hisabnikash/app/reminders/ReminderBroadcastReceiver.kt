package com.hisabnikash.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hisabnikash.app.MainActivity
import com.hisabnikash.app.R
import com.hisabnikash.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID_NOTIFICATION = "channel_reminders_notification"
        const val CHANNEL_NAME_NOTIFICATION = "রিমাইন্ডার নোটিফিকেশন"

        const val CHANNEL_ID_ALARM = "channel_reminders_alarm"
        const val CHANNEL_NAME_ALARM = "রিমাইন্ডার এ্যালার্ম"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return

        when (action) {
            ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "রিমাইন্ডার"
                val note = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE) ?: ""
                val alertType = intent.getStringExtra(ReminderScheduler.EXTRA_ALERT_TYPE) ?: "NOTIFICATION"
                val category = intent.getStringExtra(ReminderScheduler.EXTRA_CATEGORY) ?: "ব্যক্তিগত 👤"
                val eventTime = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_TIME) ?: ""

                showReminderNotification(
                    context = context,
                    reminderId = reminderId,
                    title = title,
                    note = note,
                    alertType = alertType,
                    category = category,
                    eventTime = eventTime
                )
            }

            ReminderScheduler.ACTION_COMPLETE_REMINDER -> {
                // নোটিফিকেশন ডিসমিস
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(reminderId.hashCode())

                // ডাটাবেসে সম্পন্ন হিসেবে মার্ক
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        db.reminderDao().updateReminderStatus(reminderId, true)
                        Log.d("ReminderReceiver", "Marked reminder $reminderId as completed")
                    } catch (e: Exception) {
                        Log.e("ReminderReceiver", "Error marking completed", e)
                    }
                }
            }

            ReminderScheduler.ACTION_SNOOZE_REMINDER -> {
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "রিমাইন্ডার"
                val alertType = intent.getStringExtra(ReminderScheduler.EXTRA_ALERT_TYPE) ?: "NOTIFICATION"

                // নোটিফিকেশন ডিসমিস
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(reminderId.hashCode())

                // ১০ মিনিট পর স্নুজ
                ReminderScheduler.snoozeReminder(context, reminderId, title, alertType)
                Log.d("ReminderReceiver", "Snoozed reminder $reminderId for 10 minutes")
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        note: String,
        alertType: String,
        category: String,
        eventTime: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val isAlarm = alertType == "ALARM"
        val channelId = if (isAlarm) CHANNEL_ID_ALARM else CHANNEL_ID_NOTIFICATION

        // অ্যাপ ওপেন করার ইনটেন্ট
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "reminders")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "সম্পন্ন" বাটন অ্যাকশন
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_COMPLETE_REMINDER
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_done").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "১০ মি. স্নুজ" বাটন অ্যাকশন
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE_REMINDER
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderScheduler.EXTRA_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_ALERT_TYPE, alertType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = buildString {
            if (eventTime.isNotBlank()) append("সময়: $eventTime | ")
            append("ক্যাটাগরি: $category")
            if (note.isNotBlank()) append("\n📝 $note")
        }

        val soundUri = if (isAlarm) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isAlarm) "⏰ $title" else "🔔 $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setSound(soundUri)
            .setVibrate(if (isAlarm) longArrayOf(0, 500, 200, 500, 200, 800) else longArrayOf(0, 300, 200, 300))
            .addAction(0, "✓ সম্পন্ন", completePendingIntent)
            .addAction(0, "⏰ ১০ মি. স্নুজ", snoozePendingIntent)

        if (isAlarm) {
            builder.setOngoing(false)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(reminderId.hashCode(), builder.build())
        }

        // সেভ টু নোটিফিকেশন সেন্টার
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val notif = com.hisabnikash.app.data.local.NotificationEntity(
                    title = if (isAlarm) "⏰ $title" else "🔔 $title",
                    message = bodyText,
                    timestamp = System.currentTimeMillis(),
                    type = "REMINDER",
                    isRead = false,
                    actionRoute = "reminders",
                    referenceId = reminderId,
                    priority = if (isAlarm) "HIGH" else "NORMAL"
                )
                db.notificationDao().insertNotification(notif)
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error saving reminder notification to DB", e)
            }
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // ১. অ্যালার্ম চ্যানেল (MAX importance, Loud ringtone, Vibration)
            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                CHANNEL_NAME_ALARM,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "উচ্চ গুরুত্বের রিমাইন্ডার এ্যালার্ম"
                setSound(alarmSound, audioAttributesAlarm)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
            }
            notificationManager.createNotificationChannel(alarmChannel)

            // ২. নোটিফিকেশন চ্যানেল
            val notifChannel = NotificationChannel(
                CHANNEL_ID_NOTIFICATION,
                CHANNEL_NAME_NOTIFICATION,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "সাধারণ রিমাইন্ডার নোটিফিকেশন"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(notifChannel)
        }
    }
}
