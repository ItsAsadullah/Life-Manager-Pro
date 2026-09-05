package com.hisabnikash.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hisabnikash.app.MainActivity
import com.hisabnikash.app.R
import com.hisabnikash.app.data.local.AppDatabase
import com.hisabnikash.app.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_ID_BUDGET = "budget_warning_channel"
    const val CHANNEL_NAME_BUDGET = "বাজেট সতর্কতা"

    const val CHANNEL_ID_DAILY = "daily_reminders_channel"
    const val CHANNEL_NAME_DAILY = "দৈনিক নোটিফিকেশন ও রিমাইন্ডার"

    const val CHANNEL_ID_TRANSACTIONS = "transactions_channel"
    const val CHANNEL_NAME_TRANSACTIONS = "লেনদেন আপডেট"

    const val CHANNEL_ID_GENERAL = "general_channel"
    const val CHANNEL_NAME_GENERAL = "সাধারণ নোটিফিকেশন"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                CHANNEL_NAME_BUDGET,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "বাজেট অতিক্রম বা সতর্কতামূলক নোটিফিকেশন"
                enableVibration(true)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                CHANNEL_NAME_DAILY,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "প্রতিদিনের সকাল, দুপুর ও রাতের নির্ধারিত নোটিফিকেশন"
                enableVibration(true)
            }

            val txChannel = NotificationChannel(
                CHANNEL_ID_TRANSACTIONS,
                CHANNEL_NAME_TRANSACTIONS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "স্বয়ংক্রিয় লেনদেন ও ওয়ালেট আপডেট"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "অ্যাপের অন্যান্য তথ্য ও টিপস"
            }

            notificationManager.createNotificationChannels(
                listOf(budgetChannel, dailyChannel, txChannel, generalChannel)
            )
        }
    }

    // Keep legacy single create channel for backward compatibility
    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }

    private fun Double.toBanglaString(): String {
        val format = java.text.DecimalFormat("#,##0.##")
        val enStr = format.format(this)
        val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return enStr.map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
    }

    private fun Int.toBanglaString(): String {
        val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return this.toString().map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
    }

    /**
     * Sends a system notification AND saves it to the Notification Center Room DB.
     */
    fun sendAndSaveNotification(
        context: Context,
        title: String,
        message: String,
        type: String, // REMINDER, DAILY_MORNING, DAILY_AFTERNOON, DAILY_NIGHT, BUDGET_ALERT, RECURRING_TRANSACTION, SYSTEM
        actionRoute: String? = null,
        referenceId: String? = null,
        priority: String = "NORMAL",
        channelId: String = CHANNEL_ID_GENERAL
    ) {
        // ১. ডাটাবেসে নোটিফিকেশন সেন্টারে সেভ করা
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val notificationEntity = NotificationEntity(
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    isRead = false,
                    actionRoute = actionRoute,
                    referenceId = referenceId,
                    priority = priority
                )
                db.notificationDao().insertNotification(notificationEntity)
                Log.d("NotificationHelper", "Saved notification to DB: $title")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error saving notification to DB", e)
            }
        }

        // ২. সিস্টেম স্ট্যাটাস বারে নোটিফিকেশন প্রদর্শন করা
        showSystemNotification(
            context = context,
            title = title,
            message = message,
            channelId = channelId,
            actionRoute = actionRoute
        )
    }

    private fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String,
        actionRoute: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", actionRoute ?: "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (title + message).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (channelId == CHANNEL_ID_BUDGET) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    /**
     * বাজেট সতর্কবার্তা পাঠানো ও সেভ করা
     */
    fun showBudgetWarningNotification(context: Context, category: String, budgetAmount: Double, totalSpent: Double) {
        val overAmount = totalSpent - budgetAmount
        val percentage = if (budgetAmount > 0) ((overAmount / budgetAmount) * 100).toInt() else 0

        val budgetStr = budgetAmount.toBanglaString()
        val spentStr = totalSpent.toBanglaString()
        val percentageStr = percentage.toBanglaString()

        val title = "বাজেট সতর্কতা ⚠️"
        val message = "আপনার '$category' বাজেট $budgetStr টাকা, কিন্তু খরচ হয়েছে $spentStr টাকা যা বাজেটের চেয়ে $percentageStr% বেশি!"

        sendAndSaveNotification(
            context = context,
            title = title,
            message = message,
            type = "BUDGET_ALERT",
            actionRoute = "budget",
            priority = "HIGH",
            channelId = CHANNEL_ID_BUDGET
        )
    }
}
