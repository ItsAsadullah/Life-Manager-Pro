package com.hisabnikash.app.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hisabnikash.app.auth.AuthManager
import com.hisabnikash.app.data.local.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class CloudSyncManager private constructor() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun initPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _lastSyncTime.value = prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    private fun updateLastSync(context: Context, timestamp: Long) {
        _lastSyncTime.value = timestamp
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, timestamp)
            .apply()
    }

    /**
     * অফলাইন-ফার্স্ট পূর্ণাঙ্গ সিঙ্ক:
     * ১. লোকাল Room DB থেকে ডাটা ক্লাউডে আপলোড করা
     * ২. ক্লাউড থেকে কোনো নতুন ডাটা থাকলে লোকাল Room DB-তে মার্জ করা
     */
    suspend fun sync(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val userId = authManager.currentUserId
        if (userId == null) {
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "ক্লাউড সিঙ্কের জন্য প্রথমে লগইন করুন"
            return@withContext Result.failure(Exception("ইউজার লগইন করা নেই"))
        }

        _syncStatus.value = SyncStatus.SYNCING
        _statusMessage.value = "ক্লাউডে ডাটা সিঙ্ক হচ্ছে..."

        try {
            val db = AppDatabase.getDatabase(context)
            val userRef = firestore.collection("users").document(userId)

            // ১. লোকাল ডাটা রিড করা
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
            val wallets = db.walletDao().getAllWallets().first()

            // ২. ব্যাচ আপলোড (Firestore Batched Writes - 500 limit safety)
            var currentBatch = firestore.batch()
            var opCount = 0

            suspend fun commitBatchIfNeeded() {
                if (opCount >= 400) {
                    currentBatch.commit().await()
                    currentBatch = firestore.batch()
                    opCount = 0
                }
            }

            // আপলোড: ট্রানজেকশন
            transactions.forEach { tx ->
                val doc = userRef.collection("transactions").document(tx.id)
                val data = hashMapOf(
                    "id" to tx.id,
                    "title" to tx.title,
                    "amount" to tx.amount,
                    "isIncome" to tx.isIncome,
                    "category" to tx.category,
                    "date" to tx.date,
                    "time" to tx.time,
                    "isRecurring" to tx.isRecurring,
                    "recurringPeriod" to tx.recurringPeriod,
                    "shoppingListId" to (tx.shoppingListId ?: ""),
                    "paymentMethod" to (tx.paymentMethod ?: "নগদ ক্যাশ")
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: ওয়ালেট
            wallets.forEach { w ->
                val doc = userRef.collection("wallets").document(w.id)
                val data = hashMapOf(
                    "id" to w.id,
                    "name" to w.name,
                    "accountType" to w.accountType,
                    "accountNumber" to w.accountNumber,
                    "balance" to w.balance,
                    "colorHex" to w.colorHex,
                    "isDefault" to w.isDefault,
                    "notes" to w.notes,
                    "orderIndex" to w.orderIndex
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: ব্যক্তি ও দেনা-পাওনা
            persons.forEach { p ->
                val doc = userRef.collection("persons").document(p.id)
                val data = hashMapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "phone" to p.phone,
                    "address" to p.address,
                    "dateAdded" to p.dateAdded,
                    "isDeleted" to p.isDeleted,
                    "photoUri" to (p.photoUri ?: "")
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            personTx.forEach { pt ->
                val doc = userRef.collection("person_transactions").document(pt.id)
                val data = hashMapOf(
                    "id" to pt.id,
                    "personId" to pt.personId,
                    "amount" to pt.amount,
                    "isReceive" to pt.isReceive,
                    "note" to pt.note,
                    "date" to pt.date,
                    "time" to pt.time
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: বাজেট
            budgets.forEach { b ->
                val doc = userRef.collection("budgets").document(b.id)
                val data = hashMapOf(
                    "id" to b.id,
                    "category" to b.category,
                    "monthlyLimit" to b.monthlyLimit,
                    "monthYear" to b.monthYear
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: সঞ্চয়
            savingsGoals.forEach { s ->
                val doc = userRef.collection("savings_goals").document(s.id)
                val data = hashMapOf(
                    "id" to s.id,
                    "title" to s.title,
                    "targetAmount" to s.targetAmount,
                    "currentAmount" to s.currentAmount,
                    "targetDate" to s.targetDate,
                    "category" to s.category
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            savingsTx.forEach { st ->
                val doc = userRef.collection("savings_transactions").document(st.id)
                val data = hashMapOf(
                    "id" to st.id,
                    "goalId" to st.goalId,
                    "amount" to st.amount,
                    "isDeposit" to st.isDeposit,
                    "note" to st.note,
                    "date" to st.date
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: বাজার ফর্দ
            marketLists.forEach { ml ->
                val doc = userRef.collection("market_lists").document(ml.id)
                val data = hashMapOf(
                    "id" to ml.id,
                    "title" to ml.title,
                    "date" to ml.date,
                    "budget" to ml.budget,
                    "isCompleted" to ml.isCompleted,
                    "category" to ml.category,
                    "shopName" to ml.shopName,
                    "paymentMethod" to ml.paymentMethod,
                    "linkedExpenseId" to (ml.linkedExpenseId ?: ""),
                    "completedDate" to (ml.completedDate ?: "")
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            marketItems.forEach { mi ->
                val doc = userRef.collection("market_items").document(mi.id)
                val data = hashMapOf(
                    "id" to mi.id,
                    "listId" to mi.listId,
                    "name" to mi.name,
                    "quantity" to mi.quantity,
                    "unit" to mi.unit,
                    "estimatedPrice" to mi.estimatedPrice,
                    "actualPrice" to mi.actualPrice,
                    "isPurchased" to mi.isPurchased,
                    "category" to mi.category,
                    "note" to mi.note
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: নোটস
            notes.forEach { n ->
                val doc = userRef.collection("notes").document(n.id)
                val data = hashMapOf(
                    "id" to n.id,
                    "title" to n.title,
                    "content" to n.content,
                    "category" to n.category,
                    "colorHex" to n.colorHex,
                    "isPinned" to n.isPinned,
                    "dateCreated" to n.dateCreated,
                    "dateUpdated" to n.dateUpdated
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: টাস্ক
            taskItems.forEach { t ->
                val doc = userRef.collection("tasks").document(t.id)
                val data = hashMapOf(
                    "id" to t.id,
                    "title" to t.title,
                    "note" to t.note,
                    "dueDate" to t.dueDate,
                    "isCompleted" to t.isCompleted,
                    "priority" to t.priority,
                    "dateCreated" to t.dateCreated
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // আপলোড: রিমাইন্ডার
            reminders.forEach { r ->
                val doc = userRef.collection("reminders").document(r.id)
                val data = hashMapOf(
                    "id" to r.id,
                    "title" to r.title,
                    "note" to r.note,
                    "eventDate" to r.eventDate,
                    "eventTime" to r.eventTime,
                    "timestamp" to r.timestamp,
                    "triggerTimestamp" to r.triggerTimestamp,
                    "advanceNotice" to r.advanceNotice,
                    "advanceNoticeMinutes" to r.advanceNoticeMinutes,
                    "alertType" to r.alertType,
                    "category" to r.category,
                    "priority" to r.priority,
                    "repeatInterval" to r.repeatInterval,
                    "isCompleted" to r.isCompleted,
                    "isEnabled" to r.isEnabled,
                    "dateCreated" to r.dateCreated
                )
                currentBatch.set(doc, data, SetOptions.merge())
                opCount++
                commitBatchIfNeeded()
            }

            // মেটাডাটা আপডেট
            val now = System.currentTimeMillis()
            val metaData = hashMapOf(
                "lastSyncTime" to now,
                "deviceModel" to Build.MODEL,
                "totalTransactions" to transactions.size,
                "totalWallets" to wallets.size,
                "userEmail" to (authManager.userEmail ?: "")
            )
            currentBatch.set(userRef.collection("meta").document("sync_info"), metaData, SetOptions.merge())
            opCount++

            // ফাইনাল কমিট
            if (opCount > 0) {
                currentBatch.commit().await()
            }

            updateLastSync(context, now)
            _syncStatus.value = SyncStatus.SUCCESS
            _statusMessage.value = "সকল ডাটা ক্লাউডে সফলভাবে সংরক্ষিত হয়েছে ✓"
            Log.d(TAG, "Sync complete for user $userId. Items count: $opCount")
            Result.success("সিঙ্ক সফলভাবে সম্পন্ন হয়েছে")

        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "সিঙ্ক সম্পন্ন করা যায়নি: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * নতুন ডিভাইসে লগইন করার পর ক্লাউড থেকে ডাটা রিস্টোর/ডাউনলোড করা
     */
    suspend fun restoreFromCloud(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val userId = authManager.currentUserId
            ?: return@withContext Result.failure(Exception("লগইন করা নেই"))

        _syncStatus.value = SyncStatus.SYNCING
        _statusMessage.value = "ক্লাউড থেকে হিসাব ফিরিয়ে আনা হচ্ছে..."

        try {
            val db = AppDatabase.getDatabase(context)
            val userRef = firestore.collection("users").document(userId)
            var totalRestored = 0

            // ১. ওয়ালেট ডাউনলোড
            val walletsSnap = userRef.collection("wallets").get().await()
            var hasInsertedCash = false
            walletsSnap.documents.forEach { doc ->
                val name = doc.getString("name") ?: "নগদ ক্যাশ"
                val accountType = doc.getString("accountType") ?: "CASH"
                val isCash = name == "নগদ ক্যাশ" || accountType == "CASH"

                if (isCash) {
                    if (hasInsertedCash) {
                        // ক্লাউডে পুরনো ডুপ্লিকেট ক্যাশ ওয়ালেট থাকলে তা স্কিপ ও ক্লাউড থেকে ক্লিন করা
                        deleteItemFromCloud("wallets", doc.id)
                        return@forEach
                    }
                    hasInsertedCash = true
                }

                val w = WalletEntity(
                    id = if (isCash) WalletEntity.DEFAULT_CASH_ID else (doc.getString("id") ?: doc.id),
                    name = if (isCash) "নগদ ক্যাশ" else name,
                    accountType = accountType,
                    accountNumber = doc.getString("accountNumber") ?: "",
                    balance = doc.getDouble("balance") ?: 0.0,
                    colorHex = doc.getLong("colorHex") ?: (if (isCash) 0xFF34C759 else 0xFF0A84FF),
                    isDefault = doc.getBoolean("isDefault") ?: (isCash),
                    notes = doc.getString("notes") ?: "",
                    orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt()
                )
                db.walletDao().insertWallet(w)
                totalRestored++
            }

            // ২. ট্রানজেকশন ডাউনলোড
            val txSnap = userRef.collection("transactions").get().await()
            txSnap.documents.forEach { doc ->
                val tx = TransactionEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "হিসাব",
                    amount = doc.getDouble("amount") ?: 0.0,
                    isIncome = doc.getBoolean("isIncome") ?: false,
                    category = doc.getString("category") ?: "অন্যান্য",
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: "",
                    isRecurring = doc.getBoolean("isRecurring") ?: false,
                    recurringPeriod = doc.getString("recurringPeriod") ?: "none",
                    shoppingListId = doc.getString("shoppingListId")?.ifBlank { null },
                    paymentMethod = doc.getString("paymentMethod")?.ifBlank { null }
                )
                db.transactionDao().insertTransaction(tx)
                totalRestored++
            }

            // ৩. ব্যক্তি ও ঋণ
            val personsSnap = userRef.collection("persons").get().await()
            personsSnap.documents.forEach { doc ->
                val p = PersonEntity(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    address = doc.getString("address") ?: "",
                    dateAdded = doc.getString("dateAdded") ?: "",
                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                    photoUri = doc.getString("photoUri")?.ifBlank { null }
                )
                db.personDao().insertPerson(p)
                totalRestored++
            }

            val ptxSnap = userRef.collection("person_transactions").get().await()
            ptxSnap.documents.forEach { doc ->
                val pt = PersonTransactionEntity(
                    id = doc.getString("id") ?: doc.id,
                    personId = doc.getString("personId") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    isReceive = doc.getBoolean("isReceive") ?: false,
                    note = doc.getString("note") ?: "",
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: ""
                )
                db.personTransactionDao().insertTransaction(pt)
                totalRestored++
            }

            // ৪. বাজেট
            val budgetSnap = userRef.collection("budgets").get().await()
            budgetSnap.documents.forEach { doc ->
                val b = BudgetEntity(
                    id = doc.getString("id") ?: doc.id,
                    category = doc.getString("category") ?: "",
                    monthlyLimit = doc.getDouble("monthlyLimit") ?: 0.0,
                    monthYear = doc.getString("monthYear") ?: ""
                )
                db.budgetDao().insertBudget(b)
                totalRestored++
            }

            // ৫. সঞ্চয়
            val savingsSnap = userRef.collection("savings_goals").get().await()
            savingsSnap.documents.forEach { doc ->
                val s = SavingsGoalEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                    targetDate = doc.getString("targetDate") ?: "",
                    category = doc.getString("category") ?: "সাধারণ"
                )
                db.savingsGoalDao().insertSavingsGoal(s)
                totalRestored++
            }

            val savingsTxSnap = userRef.collection("savings_transactions").get().await()
            savingsTxSnap.documents.forEach { doc ->
                val st = SavingsTransactionEntity(
                    id = doc.getString("id") ?: doc.id,
                    goalId = doc.getString("goalId") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    isDeposit = doc.getBoolean("isDeposit") ?: true,
                    note = doc.getString("note") ?: "",
                    date = doc.getString("date") ?: ""
                )
                db.savingsTransactionDao().insertSavingsTransaction(st)
                totalRestored++
            }

            // ৬. নোটস
            val notesSnap = userRef.collection("notes").get().await()
            notesSnap.documents.forEach { doc ->
                val n = NoteEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    content = doc.getString("content") ?: "",
                    category = doc.getString("category") ?: "সাধারণ",
                    colorHex = doc.getString("colorHex") ?: "#0A84FF",
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    dateCreated = doc.getString("dateCreated") ?: "",
                    dateUpdated = doc.getString("dateUpdated") ?: ""
                )
                db.noteDao().insertNote(n)
                totalRestored++
            }

            // ৭. বাজার ফর্দ
            val marketSnap = userRef.collection("market_lists").get().await()
            marketSnap.documents.forEach { doc ->
                val ml = MarketListEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    date = doc.getString("date") ?: "",
                    budget = doc.getDouble("budget") ?: 0.0,
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    category = doc.getString("category") ?: "সাধারণ",
                    shopName = doc.getString("shopName") ?: "",
                    paymentMethod = doc.getString("paymentMethod") ?: "ক্যাশ",
                    linkedExpenseId = doc.getString("linkedExpenseId")?.ifBlank { null },
                    completedDate = doc.getString("completedDate")?.ifBlank { null }
                )
                db.marketDao().insertMarketList(ml)
                totalRestored++
            }

            val itemsSnap = userRef.collection("market_items").get().await()
            itemsSnap.documents.forEach { doc ->
                val mi = MarketItemEntity(
                    id = doc.getString("id") ?: doc.id,
                    listId = doc.getString("listId") ?: "",
                    name = doc.getString("name") ?: "",
                    quantity = doc.getString("quantity") ?: "১",
                    unit = doc.getString("unit") ?: "টি",
                    estimatedPrice = doc.getDouble("estimatedPrice") ?: 0.0,
                    actualPrice = doc.getDouble("actualPrice") ?: 0.0,
                    isPurchased = doc.getBoolean("isPurchased") ?: false,
                    category = doc.getString("category") ?: "সাধারণ",
                    note = doc.getString("note") ?: ""
                )
                db.marketDao().insertMarketItem(mi)
                totalRestored++
            }

            // ৮. টাস্ক
            val tasksSnap = userRef.collection("tasks").get().await()
            tasksSnap.documents.forEach { doc ->
                val t = TaskItemEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    note = doc.getString("note") ?: "",
                    dueDate = doc.getString("dueDate") ?: "",
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    priority = doc.getString("priority") ?: "মাঝারি",
                    dateCreated = doc.getString("dateCreated") ?: ""
                )
                db.taskItemDao().insertTaskItem(t)
                totalRestored++
            }

            // ৯. রিমাইন্ডার
            val remindersSnap = userRef.collection("reminders").get().await()
            remindersSnap.documents.forEach { doc ->
                val r = ReminderEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    note = doc.getString("note") ?: "",
                    eventDate = doc.getString("eventDate") ?: "",
                    eventTime = doc.getString("eventTime") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    triggerTimestamp = doc.getLong("triggerTimestamp") ?: System.currentTimeMillis(),
                    advanceNotice = doc.getString("advanceNotice") ?: "নির্দিষ্ট সময়ে",
                    advanceNoticeMinutes = doc.getLong("advanceNoticeMinutes") ?: 0L,
                    alertType = doc.getString("alertType") ?: "NOTIFICATION",
                    category = doc.getString("category") ?: "ব্যক্তিগত 👤",
                    priority = doc.getString("priority") ?: "MEDIUM",
                    repeatInterval = doc.getString("repeatInterval") ?: "NONE",
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    isEnabled = doc.getBoolean("isEnabled") ?: true,
                    dateCreated = doc.getLong("dateCreated") ?: System.currentTimeMillis()
                )
                db.reminderDao().insertReminder(r)
                totalRestored++
            }

            val now = System.currentTimeMillis()
            updateLastSync(context, now)
            _syncStatus.value = SyncStatus.SUCCESS
            _statusMessage.value = "ক্লাউড থেকে $totalRestored টি হিসাব পুনরুদ্ধার করা হয়েছে ✓"
            Result.success(totalRestored)

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from cloud", e)
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "ডাটা পুনরুদ্ধার করা যায়নি: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * Alias for restoreFromCloud
     */
    suspend fun downloadFromCloud(context: Context): Result<Int> = restoreFromCloud(context)

    /**
     * ক্লাউড সার্ভার (Firestore) থেকে ব্যবহারকারীর সমস্ত ব্যাকআপ স্থায়ীভাবে মুছে ফেলা
     */
    suspend fun deleteUserCloudData(context: Context): Result<Unit> = withContext(Dispatchers.IO + NonCancellable) {
        val userId = authManager.currentUserId
            ?: return@withContext Result.failure(Exception("ইউজার লগইন করা নেই"))

        _syncStatus.value = SyncStatus.SYNCING
        _statusMessage.value = "ক্লাউড ব্যাকআপ মুছে ফেলা হচ্ছে..."

        try {
            val userRef = firestore.collection("users").document(userId)
            val subcollections = listOf(
                "transactions",
                "wallets",
                "persons",
                "person_transactions",
                "budgets",
                "savings_goals",
                "savings_transactions",
                "market_lists",
                "market_items",
                "notes",
                "tasks",
                "reminders",
                "quick_entries",
                "notifications",
                "meta"
            )

            for (colName in subcollections) {
                try {
                    val snapshot = userRef.collection(colName).get().await()
                    var batch = firestore.batch()
                    var count = 0
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                        count++
                        if (count >= 400) {
                            batch.commit().await()
                            batch = firestore.batch()
                            count = 0
                        }
                    }
                    if (count > 0) {
                        batch.commit().await()
                    }
                    Log.d(TAG, "Cleared cloud subcollection: $colName")
                } catch (ce: Exception) {
                    Log.w(TAG, "Error clearing subcollection $colName", ce)
                }
            }

            // ব্যবহারকারীর মূল ডকুমেন্ট মুছে ফেলা
            try {
                userRef.delete().await()
            } catch (ue: Exception) {
                Log.w(TAG, "Error deleting root user document", ue)
            }

            updateLastSync(context, 0L)
            _syncStatus.value = SyncStatus.IDLE
            _statusMessage.value = "ক্লাউড থেকে সমস্ত ডাটা চিরতরে মুছে ফেলা হয়েছে"
            Log.d(TAG, "Successfully wiped cloud data for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping cloud data", e)
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "ক্লাউড ডাটা মুছতে সমস্যা হয়েছে: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * ক্লাউড থেকে নির্দিষ্ট একটি আইটেম তাৎক্ষণিকভাবে মুছে ফেলা (যাতে ভবিষ্যতে সিঙ্কে তা ফিরে না আসে)
     */
    fun deleteItemFromCloud(collectionName: String, itemId: String) {
        val userId = authManager.currentUserId ?: return
        CoroutineScope(Dispatchers.IO + NonCancellable).launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection(collectionName)
                    .document(itemId)
                    .delete()
                    .await()
                Log.d(TAG, "Item $itemId deleted from cloud collection $collectionName")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete item $itemId from cloud collection $collectionName", e)
            }
        }
    }

    /**
     * অ্যাপ ওপেন করার সময় সাধারণ সিঙ্ক:
     * ডিভাইসে লেনদেন বা ডাটা থাকলে তা ক্লাউডে আপলোড/সিঙ্ক করবে।
     * ফাঁকা ডাটাবেসে স্বয়ংক্রিয়ভাবে রিস্টোর করবে না (যাতে ব্যবহারকারীর ডিলিট করা ডাটা আবার ফিরে না আসে)।
     */
    suspend fun syncIfHasData(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val userId = authManager.currentUserId ?: return@withContext Result.failure(Exception("ইউজার লগইন নেই"))
        try {
            val db = AppDatabase.getDatabase(context)
            val localTx = db.transactionDao().getAllTransactions().first()
            val localPersons = db.personDao().getAllPersons().first()
            val localMarket = db.marketDao().getAllMarketLists().first()
            val localNotes = db.noteDao().getAllNotes().first()

            val hasLocalData = localTx.isNotEmpty() || localPersons.isNotEmpty() || localMarket.isNotEmpty() || localNotes.isNotEmpty()
            if (hasLocalData) {
                sync(context)
            } else {
                Log.d(TAG, "Local database is empty. Skipping auto-restore on app open to respect data deletion.")
                Result.success("ফাঁকা ডাটাবেস, রিস্টোর বাদ দেওয়া হয়েছে")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncIfHasData", e)
            Result.failure(e)
        }
    }

    /**
     * লগইনের পর স্মার্ট সিঙ্ক:
     * ডিভাইসে যদি কোনো ডাটা না থাকে (ক্লিয়ার করা বা নতুন ডিভাইস), তবে ক্লাউড থেকে স্বয়ংক্রিয়ভাবে রিস্টোর করবে।
     * আর ডিভাইসে আগে থেকেই ডাটা থাকলে দুইমুখী সিঙ্ক সম্পন্ন করবে।
     */
    suspend fun smartSyncOnLogin(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val userId = authManager.currentUserId
            ?: return@withContext Result.failure(Exception("ইউজার লগইন করা নেই"))

        try {
            val db = AppDatabase.getDatabase(context)
            val localTx = db.transactionDao().getAllTransactions().first()
            val localWallets = db.walletDao().getAllWallets().first()
            val localPersons = db.personDao().getAllPersons().first()
            val localMarket = db.marketDao().getAllMarketLists().first()
            val localNotes = db.noteDao().getAllNotes().first()

            val isLocalClean = localTx.isEmpty() && 
                    localPersons.isEmpty() && 
                    localMarket.isEmpty() && 
                    localNotes.isEmpty() &&
                    (localWallets.isEmpty() || (localWallets.size == 1 && localWallets[0].name == "নগদ ক্যাশ" && localWallets[0].balance == 0.0))

            if (isLocalClean) {
                // ক্লাউডে কোনো ডাটা আছে কি না চেক করা
                val userRef = firestore.collection("users").document(userId)
                val cloudTx = userRef.collection("transactions").limit(1).get().await()
                val cloudWallets = userRef.collection("wallets").limit(1).get().await()

                if (!cloudTx.isEmpty || !cloudWallets.isEmpty) {
                    Log.d(TAG, "Local DB is clean, restoring from cloud automatically...")
                    return@withContext restoreFromCloud(context)
                }
            }

            // অন্যথায় স্বাভাবিক সিঙ্ক সম্পন্ন করা
            val syncRes = sync(context)
            if (syncRes.isSuccess) {
                Result.success(0)
            } else {
                Result.failure(syncRes.exceptionOrNull() ?: Exception("সিঙ্ক ব্যর্থ"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in smartSyncOnLogin", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "CloudSyncManager"
        private const val PREFS_NAME = "hisab_nikash_sync"
        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"

        @Volatile
        private var INSTANCE: CloudSyncManager? = null

        fun getInstance(): CloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudSyncManager().also { INSTANCE = it }
            }
        }
    }
}
