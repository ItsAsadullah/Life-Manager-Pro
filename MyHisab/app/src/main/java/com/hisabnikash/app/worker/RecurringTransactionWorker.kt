package com.hisabnikash.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.data.local.AppDatabase
import com.hisabnikash.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RecurringWorker", "Running recurring transaction check...")
        val dao = AppDatabase.getDatabase(applicationContext).transactionDao()
        val recurringTransactions = dao.getRecurringTransactions()

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
        val todayText = dateFormat.format(calendar.time)

        for (tx in recurringTransactions) {
            // Very simplified logic: create a new entry for today based on recurring period.
            // In a real app, you would check if an entry for today/this week was already generated.
            val newTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = tx.title + " (Auto)",
                amount = tx.amount,
                isIncome = tx.isIncome,
                time = tx.time,
                date = todayText,
                category = tx.category,
                isRecurring = false,
                recurringPeriod = null,
                paymentMethod = tx.paymentMethod ?: "নগদ ক্যাশ"
            )
            dao.insertTransaction(newTx)
            Log.d("RecurringWorker", "Generated new transaction from recurring: ${tx.title}")

            com.hisabnikash.app.utils.NotificationHelper.sendAndSaveNotification(
                context = applicationContext,
                title = "স্বয়ংক্রিয় লেনদেন সম্পন্ন 🔄",
                message = "'${tx.title}' বাবদ ${if (tx.isIncome) "আয়" else "খরচ"} হিসেবে ৳${tx.amount.toInt()} যুক্ত হয়েছে।",
                type = "RECURRING_TRANSACTION",
                actionRoute = "dashboard",
                priority = "NORMAL",
                channelId = com.hisabnikash.app.utils.NotificationHelper.CHANNEL_ID_TRANSACTIONS
            )
        }

        return Result.success()
    }
}
