package com.hisabnikash.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.data.local.AppDatabase
import com.hisabnikash.app.utils.NotificationHelper
import com.hisabnikash.app.utils.SettingsPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyNotificationWorker", "Running daily notification check...")
        checkAndDispatchDailyNotifications(applicationContext)
        return Result.success()
    }

    companion object {
        private fun toBanglaDigits(number: Int): String {
            val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            return number.toString().map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
        }

        suspend fun checkAndDispatchDailyNotifications(context: Context) {
            try {
                val settingsPrefs = SettingsPreferences(context)
                if (!settingsPrefs.dailyReminderEnabled.value) {
                    Log.d("DailyNotificationWorker", "Daily notifications disabled by user")
                    return
                }

                val db = AppDatabase.getDatabase(context)
                val notificationDao = db.notificationDao()
                val transactionDao = db.transactionDao()
                val reminderDao = db.reminderDao()
                val budgetDao = db.budgetDao()

                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                // আজকের শুরু (00:00:00) ও শেষ (23:59:59)
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = startCal.timeInMillis

                val endCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfDay = endCal.timeInMillis

                val bnDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                val todayBn = bnDateFormat.format(calendar.time)
                val enDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("en", "US"))
                val todayEn = enDateFormat.format(calendar.time)

                // ১. সকালের নোটিফিকেশন (সকাল ৯:০০ - দুপুর ১:৫৯)
                if (currentHour in 9..13) {
                    val alreadySent = notificationDao.hasNotificationForDayAndType("DAILY_MORNING", startOfDay, endOfDay) > 0
                    if (!alreadySent) {
                        val activeReminders = reminderDao.getActiveRemindersSync()
                        val todayReminders = activeReminders.filter { it.eventDate == todayBn || it.eventDate == todayEn }

                        val (title, message, route) = if (todayReminders.isNotEmpty()) {
                            val firstTitle = todayReminders.first().title
                            val extraText = if (todayReminders.size > 1) " সহ আরও ${toBanglaDigits(todayReminders.size - 1)}টি" else ""
                            Triple(
                                "সুপ্রভাত! আজকের রিমাইন্ডার ⏰",
                                "আজ আপনার নির্ধারিত কাজ/রিমাইন্ডার: '$firstTitle'$extraText। দিনের শুরুতেই প্রস্তুতি নিন!",
                                "reminders"
                            )
                        } else {
                            val budgets = budgetDao.getAllBudgetsList()
                            val totalBudget = budgets.sumOf { it.monthlyLimit }
                            if (totalBudget > 0) {
                                Triple(
                                    "সুপ্রভাত! আজকের আয়-ব্যয় পরিকল্পনা ☀️",
                                    "দিনের শুরুতে আপনার সম্ভাব্য খরচ ও বাজেট পরিকল্পনা ঠিক করে নিন। চলতি মাসে মোট বাজেট ৳${toBanglaDigits(totalBudget.toInt())}।",
                                    "budget"
                                )
                            } else {
                                Triple(
                                    "সুপ্রভাত! আজকের আয়-ব্যয় পরিকল্পনা ☀️",
                                    "দিনের শুরুতে আপনার সম্ভাব্য খরচ ও বাজেট পরিকল্পনা ঠিক করে নিন। সচেতন থাকুন, সঞ্চয় করুন।",
                                    "dashboard"
                                )
                            }
                        }

                        NotificationHelper.sendAndSaveNotification(
                            context = context,
                            title = title,
                            message = message,
                            type = "DAILY_MORNING",
                            actionRoute = route,
                            priority = "NORMAL",
                            channelId = NotificationHelper.CHANNEL_ID_DAILY
                        )
                        Log.d("DailyNotificationWorker", "Morning notification dispatched: $title")
                    }
                }

                // ২. দুপুরের নোটিফিকেশন (দুপুর ২:০০ - সন্ধ্যা ৭:৫৯)
                if (currentHour in 14..19) {
                    val alreadySent = notificationDao.hasNotificationForDayAndType("DAILY_AFTERNOON", startOfDay, endOfDay) > 0
                    if (!alreadySent) {
                        val allTx = transactionDao.getAllTransactionsList()
                        val todayTx = allTx.filter { it.date == todayBn || it.date == todayEn }
                        val todaySpent = todayTx.filter { !it.isIncome }.sumOf { it.amount }

                        val message = if (todaySpent > 0) {
                            "আজ এখন পর্যন্ত মোট ৳${toBanglaDigits(todaySpent.toInt())} খরচ হয়েছে। দুপুরের আর কোনো খরচ থাকলে যোগ করে নিন।"
                        } else {
                            "আজ দুপুর পর্যন্ত কি কোনো খরচ হয়েছে? এখনই হিসাব নিকাশে যোগ করে হালনাগাদ রাখুন।"
                        }

                        NotificationHelper.sendAndSaveNotification(
                            context = context,
                            title = "দুপুরের খরচের হিসাব ☕",
                            message = message,
                            type = "DAILY_AFTERNOON",
                            actionRoute = "dashboard",
                            priority = "NORMAL",
                            channelId = NotificationHelper.CHANNEL_ID_DAILY
                        )
                        Log.d("DailyNotificationWorker", "Afternoon notification dispatched")
                    }
                }

                // ৩. রাতের নোটিফিকেশন (রাত ৮:০০ - রাত ১১:৫৯)
                if (currentHour >= 20) {
                    val alreadySent = notificationDao.hasNotificationForDayAndType("DAILY_NIGHT", startOfDay, endOfDay) > 0
                    if (!alreadySent) {
                        val allTx = transactionDao.getAllTransactionsList()
                        val todayTx = allTx.filter { it.date == todayBn || it.date == todayEn }
                        val todaySpent = todayTx.filter { !it.isIncome }.sumOf { it.amount }
                        val todayIncome = todayTx.filter { it.isIncome }.sumOf { it.amount }

                        val message = if (todayTx.isNotEmpty()) {
                            "আজকের মোট আয় ৳${toBanglaDigits(todayIncome.toInt())} এবং মোট ব্যয় ৳${toBanglaDigits(todaySpent.toInt())}। ঘুমানোর আগে সারাদিনের হিসাব মিলিয়ে নিন।"
                        } else {
                            "আজকের সারাদিনের খরচের কোনো হিসাব কি বাদ পড়েছে? ঘুমানোর আগে হিসাবের খাতা আপডেট রাখুন।"
                        }

                        NotificationHelper.sendAndSaveNotification(
                            context = context,
                            title = "সারাদিনের হিসাব মিলিয়ে নিন 🌙",
                            message = message,
                            type = "DAILY_NIGHT",
                            actionRoute = "dashboard",
                            priority = "NORMAL",
                            channelId = NotificationHelper.CHANNEL_ID_DAILY
                        )
                        Log.d("DailyNotificationWorker", "Night notification dispatched")
                    }
                }

            } catch (e: Exception) {
                Log.e("DailyNotificationWorker", "Error checking daily notifications", e)
            }
        }
    }
}
