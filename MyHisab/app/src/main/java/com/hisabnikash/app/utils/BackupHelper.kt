package com.hisabnikash.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.hisabnikash.app.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val TAG = "BackupHelper"

    /**
     * সম্পূর্ণ ডাটাবেস থেকে একটি সুরক্ষিত JSON ব্যাকআপ ফাইল তৈরি করে
     */
    suspend fun createFullBackupJson(context: Context): Pair<File, Int> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)

        val transactions = db.transactionDao().getAllTransactions().first()
        val quickEntries = db.quickEntryDao().getAllQuickEntries().first()
        val persons = db.personDao().getAllPersons().first()
        val personTx = db.personTransactionDao().getAllPersonTransactions().first()
        val marketLists = db.marketDao().getAllMarketLists().first()
        val marketItems = db.marketDao().getAllMarketItems().first()
        val notes = db.noteDao().getAllNotes().first()
        val budgets = db.budgetDao().getAllBudgets().first()
        val savingsGoals = db.savingsGoalDao().getAllSavingsGoals().first()
        val savingsTx = db.savingsTransactionDao().getAllSavingsTransactions().first()
        val taskItems = db.taskItemDao().getAllTaskItems().first()
        val reminders = db.reminderDao().getAllReminders().first()

        val rootJson = JSONObject().apply {
            put("app", "LifeManagerPro - Hisab Nikash")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // ১. লেনদেন (Transactions)
            val txArray = JSONArray()
            transactions.forEach { t ->
                txArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("amount", t.amount)
                    put("isIncome", t.isIncome)
                    put("category", t.category)
                    put("date", t.date)
                    put("time", t.time)
                    put("isRecurring", t.isRecurring)
                    put("recurringPeriod", t.recurringPeriod)
                    put("shoppingListId", t.shoppingListId ?: "")
                    put("paymentMethod", t.paymentMethod ?: "")
                })
            }
            put("transactions", txArray)

            // ২. কুইক এন্ট্রি (Quick Entries)
            val qeArray = JSONArray()
            quickEntries.forEach { q ->
                qeArray.put(JSONObject().apply {
                    put("id", q.id)
                    put("title", q.title)
                    put("amount", q.amount)
                    put("isIncome", q.isIncome)
                    put("category", q.category)
                    put("orderIndex", q.orderIndex)
                })
            }
            put("quick_entries", qeArray)

            // ৩. ব্যক্তি ও দেনা-পাওনা (Persons)
            val pArray = JSONArray()
            persons.forEach { p ->
                pArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("phone", p.phone)
                    put("address", p.address)
                    put("dateAdded", p.dateAdded)
                    put("isDeleted", p.isDeleted)
                    put("photoUri", p.photoUri ?: "")
                })
            }
            put("persons", pArray)

            // ৪. ব্যক্তির লেনদেন (Person Transactions)
            val ptxArray = JSONArray()
            personTx.forEach { pt ->
                ptxArray.put(JSONObject().apply {
                    put("id", pt.id)
                    put("personId", pt.personId)
                    put("amount", pt.amount)
                    put("isReceive", pt.isReceive)
                    put("note", pt.note)
                    put("date", pt.date)
                    put("time", pt.time)
                })
            }
            put("person_transactions", ptxArray)

            // ৫. বাজার ফর্দ (Market Lists)
            val mlArray = JSONArray()
            marketLists.forEach { ml ->
                mlArray.put(JSONObject().apply {
                    put("id", ml.id)
                    put("title", ml.title)
                    put("date", ml.date)
                    put("budget", ml.budget)
                    put("isCompleted", ml.isCompleted)
                    put("category", ml.category)
                    put("shopName", ml.shopName)
                    put("paymentMethod", ml.paymentMethod)
                    put("linkedExpenseId", ml.linkedExpenseId ?: "")
                    put("completedDate", ml.completedDate ?: "")
                })
            }
            put("market_lists", mlArray)

            // ৬. বাজারের পণ্য (Market Items)
            val miArray = JSONArray()
            marketItems.forEach { mi ->
                miArray.put(JSONObject().apply {
                    put("id", mi.id)
                    put("listId", mi.listId)
                    put("name", mi.name)
                    put("quantity", mi.quantity)
                    put("unit", mi.unit)
                    put("estimatedPrice", mi.estimatedPrice)
                    put("actualPrice", mi.actualPrice)
                    put("isPurchased", mi.isPurchased)
                    put("category", mi.category)
                    put("note", mi.note)
                })
            }
            put("market_items", miArray)

            // ৭. নোটস (Notes)
            val notesArray = JSONArray()
            notes.forEach { n ->
                notesArray.put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("category", n.category)
                    put("colorHex", n.colorHex)
                    put("isPinned", n.isPinned)
                    put("dateCreated", n.dateCreated)
                    put("dateUpdated", n.dateUpdated)
                })
            }
            put("notes", notesArray)

            // ৮. বাজেট (Budgets)
            val bArray = JSONArray()
            budgets.forEach { b ->
                bArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("monthlyLimit", b.monthlyLimit)
                    put("monthYear", b.monthYear)
                })
            }
            put("budgets", bArray)

            // ৯. সঞ্চয় লক্ষ্য (Savings Goals)
            val sgArray = JSONArray()
            savingsGoals.forEach { sg ->
                sgArray.put(JSONObject().apply {
                    put("id", sg.id)
                    put("title", sg.title)
                    put("targetAmount", sg.targetAmount)
                    put("currentAmount", sg.currentAmount)
                    put("targetDate", sg.targetDate)
                    put("category", sg.category)
                })
            }
            put("savings_goals", sgArray)

            // ১০. সঞ্চয় লেনদেন (Savings Transactions)
            val stArray = JSONArray()
            savingsTx.forEach { st ->
                stArray.put(JSONObject().apply {
                    put("id", st.id)
                    put("goalId", st.goalId)
                    put("amount", st.amount)
                    put("isDeposit", st.isDeposit)
                    put("note", st.note)
                    put("date", st.date)
                })
            }
            put("savings_transactions", stArray)

            // ১১. টাস্ক (Tasks)
            val taskArray = JSONArray()
            taskItems.forEach { t ->
                taskArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("note", t.note)
                    put("dueDate", t.dueDate)
                    put("isCompleted", t.isCompleted)
                    put("priority", t.priority)
                    put("dateCreated", t.dateCreated)
                })
            }
            put("task_items", taskArray)

            // ১২. রিমাইন্ডার (Reminders)
            val remArray = JSONArray()
            reminders.forEach { r ->
                remArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("note", r.note)
                    put("eventDate", r.eventDate)
                    put("eventTime", r.eventTime)
                    put("timestamp", r.timestamp)
                    put("triggerTimestamp", r.triggerTimestamp)
                    put("advanceNotice", r.advanceNotice)
                    put("advanceNoticeMinutes", r.advanceNoticeMinutes)
                    put("alertType", r.alertType)
                    put("category", r.category)
                    put("priority", r.priority)
                    put("repeatInterval", r.repeatInterval)
                    put("isCompleted", r.isCompleted)
                    put("isEnabled", r.isEnabled)
                    put("dateCreated", r.dateCreated)
                })
            }
            put("reminders", remArray)

            // ১৩. ওয়ালেট ও অ্যাকাউন্ট
            val wallets = db.walletDao().getAllWallets().first()
            val walArray = JSONArray()
            wallets.forEach { w ->
                walArray.put(JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("accountType", w.accountType)
                    put("accountNumber", w.accountNumber)
                    put("balance", w.balance)
                    put("colorHex", w.colorHex)
                    put("isDefault", w.isDefault)
                    put("notes", w.notes)
                    put("orderIndex", w.orderIndex)
                })
            }
            put("wallets", walArray)
        }

        val totalRecords = transactions.size + quickEntries.size + persons.size +
                personTx.size + marketLists.size + marketItems.size + notes.size +
                budgets.size + savingsGoals.size + savingsTx.size + taskItems.size + reminders.size +
                db.walletDao().getAllWallets().first().size

        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "HisabNikash_Backup_$dateStamp.json")

        FileOutputStream(backupFile).use { it.write(rootJson.toString(2).toByteArray()) }

        Pair(backupFile, totalRecords)
    }

    /**
     * JSON ব্যাকআপ ফাইল থেকে ডাটাবেস রিস্টোর করে
     */
    suspend fun restoreDatabaseFromJson(context: Context, jsonString: String): Int = withContext(Dispatchers.IO) {
        val rootJson = JSONObject(jsonString)
        val db = AppDatabase.getDatabase(context)
        var restoredCount = 0

        // ১. ট্রানজ্যাকশন
        if (rootJson.has("transactions")) {
            val arr = rootJson.getJSONArray("transactions")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val t = TransactionEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    isIncome = obj.getBoolean("isIncome"),
                    category = obj.getString("category"),
                    date = obj.getString("date"),
                    time = obj.getString("time"),
                    isRecurring = obj.optBoolean("isRecurring", false),
                    recurringPeriod = obj.optString("recurringPeriod", "none"),
                    shoppingListId = obj.optString("shoppingListId").ifBlank { null },
                    paymentMethod = obj.optString("paymentMethod").ifBlank { null }
                )
                db.transactionDao().insertTransaction(t)
                restoredCount++
            }
        }

        // ২. কুইক এন্ট্রি
        if (rootJson.has("quick_entries")) {
            val arr = rootJson.getJSONArray("quick_entries")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val q = QuickEntryEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    isIncome = obj.getBoolean("isIncome"),
                    category = obj.getString("category"),
                    orderIndex = obj.optInt("orderIndex", 0)
                )
                db.quickEntryDao().insertQuickEntry(q)
                restoredCount++
            }
        }

        // ৩. ব্যক্তি
        if (rootJson.has("persons")) {
            val arr = rootJson.getJSONArray("persons")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val p = PersonEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    address = obj.optString("address", ""),
                    dateAdded = obj.optString("dateAdded", ""),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    photoUri = obj.optString("photoUri").ifBlank { null }
                )
                db.personDao().insertPerson(p)
                restoredCount++
            }
        }

        // ৪. ব্যক্তির ট্রানজ্যাকশন
        if (rootJson.has("person_transactions")) {
            val arr = rootJson.getJSONArray("person_transactions")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pt = PersonTransactionEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    personId = obj.getString("personId"),
                    amount = obj.getDouble("amount"),
                    isReceive = obj.getBoolean("isReceive"),
                    note = obj.optString("note", ""),
                    date = obj.getString("date"),
                    time = obj.getString("time")
                )
                db.personTransactionDao().insertTransaction(pt)
                restoredCount++
            }
        }

        // ৫. বাজার ফর্দ
        if (rootJson.has("market_lists")) {
            val arr = rootJson.getJSONArray("market_lists")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ml = MarketListEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    date = obj.getString("date"),
                    budget = obj.optDouble("budget", 0.0),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    category = obj.optString("category", "সাধারণ"),
                    shopName = obj.optString("shopName", ""),
                    paymentMethod = obj.optString("paymentMethod", "ক্যাশ"),
                    linkedExpenseId = obj.optString("linkedExpenseId").ifBlank { null },
                    completedDate = obj.optString("completedDate").ifBlank { null }
                )
                db.marketDao().insertMarketList(ml)
                restoredCount++
            }
        }

        // ৬. বাজার আইটেম
        if (rootJson.has("market_items")) {
            val arr = rootJson.getJSONArray("market_items")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val mi = MarketItemEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    listId = obj.getString("listId"),
                    name = obj.getString("name"),
                    quantity = obj.optString("quantity", "১"),
                    unit = obj.optString("unit", "কেজি"),
                    estimatedPrice = obj.optDouble("estimatedPrice", 0.0),
                    actualPrice = obj.optDouble("actualPrice", 0.0),
                    isPurchased = obj.optBoolean("isPurchased", false),
                    category = obj.optString("category", "শাকসবজি 🥬"),
                    note = obj.optString("note", "")
                )
                db.marketDao().insertMarketItem(mi)
                restoredCount++
            }
        }

        // ৭. নোটস
        if (rootJson.has("notes")) {
            val arr = rootJson.getJSONArray("notes")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val n = NoteEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    category = obj.optString("category", "সাধারণ"),
                    colorHex = obj.optString("colorHex", "#0A84FF"),
                    isPinned = obj.optBoolean("isPinned", false),
                    dateCreated = obj.optString("dateCreated", ""),
                    dateUpdated = obj.optString("dateUpdated", "")
                )
                db.noteDao().insertNote(n)
                restoredCount++
            }
        }

        // ৮. বাজেট
        if (rootJson.has("budgets")) {
            val arr = rootJson.getJSONArray("budgets")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val b = BudgetEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    category = obj.getString("category"),
                    monthlyLimit = obj.getDouble("monthlyLimit"),
                    monthYear = obj.getString("monthYear")
                )
                db.budgetDao().insertBudget(b)
                restoredCount++
            }
        }

        // ৯. সঞ্চয় লক্ষ্য
        if (rootJson.has("savings_goals")) {
            val arr = rootJson.getJSONArray("savings_goals")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sg = SavingsGoalEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    targetAmount = obj.getDouble("targetAmount"),
                    currentAmount = obj.optDouble("currentAmount", 0.0),
                    targetDate = obj.optString("targetDate", ""),
                    category = obj.optString("category", "সাধারণ")
                )
                db.savingsGoalDao().insertSavingsGoal(sg)
                restoredCount++
            }
        }

        // ১০. সঞ্চয় ট্রানজ্যাকশন
        if (rootJson.has("savings_transactions")) {
            val arr = rootJson.getJSONArray("savings_transactions")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val st = SavingsTransactionEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    goalId = obj.getString("goalId"),
                    amount = obj.getDouble("amount"),
                    isDeposit = obj.getBoolean("isDeposit"),
                    note = obj.optString("note", ""),
                    date = obj.getString("date")
                )
                db.savingsTransactionDao().insertSavingsTransaction(st)
                restoredCount++
            }
        }

        // ১১. টাস্ক
        if (rootJson.has("task_items")) {
            val arr = rootJson.getJSONArray("task_items")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val t = TaskItemEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    note = obj.optString("note", ""),
                    dueDate = obj.optString("dueDate", ""),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    priority = obj.optString("priority", "মাঝারি"),
                    dateCreated = obj.optString("dateCreated", "")
                )
                db.taskItemDao().insertTaskItem(t)
                restoredCount++
            }
        }

        // ১২. রিমাইন্ডার
        if (rootJson.has("reminders")) {
            val arr = rootJson.getJSONArray("reminders")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val r = ReminderEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    note = obj.optString("note", ""),
                    eventDate = obj.getString("eventDate"),
                    eventTime = obj.getString("eventTime"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    triggerTimestamp = obj.optLong("triggerTimestamp", System.currentTimeMillis()),
                    advanceNotice = obj.optString("advanceNotice", "নির্দিষ্ট সময়ে"),
                    advanceNoticeMinutes = obj.optLong("advanceNoticeMinutes", 0L),
                    alertType = obj.optString("alertType", "NOTIFICATION"),
                    category = obj.optString("category", "ব্যক্তিগত 👤"),
                    priority = obj.optString("priority", "MEDIUM"),
                    repeatInterval = obj.optString("repeatInterval", "NONE"),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    dateCreated = obj.optLong("dateCreated", System.currentTimeMillis())
                )
                db.reminderDao().insertReminder(r)
                restoredCount++
            }
        }

        // ১৩. ওয়ালেট ও অ্যাকাউন্ট
        if (rootJson.has("wallets")) {
            val arr = rootJson.getJSONArray("wallets")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val w = WalletEntity(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.getString("name"),
                    accountType = obj.optString("accountType", "CASH"),
                    accountNumber = obj.optString("accountNumber", ""),
                    balance = obj.optDouble("balance", 0.0),
                    colorHex = obj.optLong("colorHex", 0xFF0A84FF),
                    isDefault = obj.optBoolean("isDefault", false),
                    notes = obj.optString("notes", ""),
                    orderIndex = obj.optInt("orderIndex", 0)
                )
                db.walletDao().insertWallet(w)
                restoredCount++
            }
        }

        restoredCount
    }

    /**
     * সমস্ত লেনদেন এক্সেল / CSV ফরম্যাটে এক্সপোর্ট করে ফাইল রিটার্ন করে
     */
    suspend fun exportTransactionsToCsv(context: Context): Pair<File, Int> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val transactions = db.transactionDao().getAllTransactions().first()

        val sb = StringBuilder()
        sb.append("তারিখ,সময়,লেনদেনের ধরন,ক্যাটাগরি,টাকার পরিমাণ (৳),বিবরণ,পেমেন্ট মাধ্যম\n")

        transactions.forEach { t ->
            val typeStr = if (t.isIncome) "আয়" else "ব্যয়"
            val titleClean = t.title.replace(",", " ")
            val catClean = t.category.replace(",", " ")
            val payClean = (t.paymentMethod ?: "ক্যাশ").replace(",", " ")
            sb.append("${t.date},${t.time},$typeStr,$catClean,${t.amount},$titleClean,$payClean\n")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(exportDir, "HisabNikash_Transactions_$dateStamp.csv")

        FileOutputStream(csvFile).use { it.write(sb.toString().toByteArray()) }

        Pair(csvFile, transactions.size)
    }

    /**
     * অ্যান্ড্রয়েড সিস্টেম শেয়ার শিট ওপেন করে ফাইল পাঠায়
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * ডাটাবেস ও ক্যাশ সাইজ তথ্য সংগ্রহ করে
     */
    suspend fun getStorageInfo(context: Context): Pair<String, String> = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("hisabnikash_database")
        val dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val dbSizeMb = String.format(Locale.getDefault(), "%.2f MB", dbSizeBytes / (1024.0 * 1024.0))

        var cacheSizeBytes = 0L
        try {
            val cacheDir = context.cacheDir
            cacheDir.walkTopDown().forEach { if (it.isFile) cacheSizeBytes += it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
        }
        val cacheSizeMb = String.format(Locale.getDefault(), "%.2f MB", cacheSizeBytes / (1024.0 * 1024.0))

        Pair(dbSizeMb, cacheSizeMb)
    }

    /**
     * অ্যাপের ক্যাশ মেমরি পরিষ্কার করে
     */
    fun clearAppCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            false
        }
    }

    /**
     * সম্পূর্ণ ডাটাবেস রিসেট করে (ফ্যাক্টরি রিসেট)
     */
    suspend fun resetAllDatabaseData(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
        // ডিফল্ট নগদ ক্যাশ অ্যাকাউন্ট বজায় রাখা যাতে অ্যাপের ব্যালেন্স স্টেট সুস্থ থাকে
        val defaultWallet = WalletEntity(
            id = "wallet_cash_default",
            name = "নগদ ক্যাশ",
            accountType = "CASH",
            accountNumber = "",
            balance = 0.0,
            colorHex = 0xFF34C759,
            isDefault = true,
            notes = "ডিফল্ট অ্যাকাউন্ট",
            orderIndex = 0
        )
        db.walletDao().insertWallet(defaultWallet)
    }
}
