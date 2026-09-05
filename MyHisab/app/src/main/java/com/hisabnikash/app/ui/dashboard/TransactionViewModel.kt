package com.hisabnikash.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.data.local.AppDatabase
import com.hisabnikash.app.data.local.QuickEntryEntity
import com.hisabnikash.app.data.local.TransactionEntity
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.data.local.PersonTransactionEntity
import com.hisabnikash.app.data.local.MarketListEntity
import com.hisabnikash.app.data.local.MarketItemEntity
import com.hisabnikash.app.data.local.NoteEntity
import com.hisabnikash.app.data.local.BudgetEntity
import com.hisabnikash.app.data.local.SavingsGoalEntity
import com.hisabnikash.app.data.local.SavingsTransactionEntity
import com.hisabnikash.app.data.local.TaskItemEntity
import com.hisabnikash.app.data.local.ReminderEntity
import com.hisabnikash.app.data.local.WalletEntity
import com.hisabnikash.app.data.local.NotificationEntity
import com.hisabnikash.app.data.local.TransactionRepository
import com.hisabnikash.app.reminders.ReminderScheduler
import com.hisabnikash.app.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    
    val allTransactions: StateFlow<List<TransactionEntity>>
    val allQuickEntries: StateFlow<List<QuickEntryEntity>>
    
    val allPersons: StateFlow<List<PersonEntity>>
    val allPersonTransactions: StateFlow<List<PersonTransactionEntity>>
    val deletedPersons: StateFlow<List<PersonEntity>>

    val allMarketLists: StateFlow<List<MarketListEntity>>
    val allNotes: StateFlow<List<NoteEntity>>
    val allBudgets: StateFlow<List<BudgetEntity>>
    val allSavingsGoals: StateFlow<List<SavingsGoalEntity>>
    val allTaskItems: StateFlow<List<TaskItemEntity>>
    val allReminders: StateFlow<List<ReminderEntity>>
    val allWallets: StateFlow<List<WalletEntity>>
    val allNotifications: StateFlow<List<NotificationEntity>>
    val unreadNotificationCount: StateFlow<Int>

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        val quickEntryDao = db.quickEntryDao()
        val personDao = db.personDao()
        val personTransactionDao = db.personTransactionDao()
        val marketDao = db.marketDao()
        val noteDao = db.noteDao()
        val budgetDao = db.budgetDao()
        val savingsGoalDao = db.savingsGoalDao()
        val savingsTransactionDao = db.savingsTransactionDao()
        val taskItemDao = db.taskItemDao()
        val reminderDao = db.reminderDao()
        val walletDao = db.walletDao()
        val notificationDao = db.notificationDao()

        repository = TransactionRepository(
            transactionDao, quickEntryDao, personDao, personTransactionDao,
            marketDao, noteDao, budgetDao, savingsGoalDao, savingsTransactionDao, taskItemDao,
            reminderDao, walletDao, notificationDao
        )

        allNotifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        unreadNotificationCount = repository.unreadNotificationCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        allWallets = repository.allWallets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // প্রাথমিক অবস্থায় কোনো ওয়ালেট না থাকলেও সর্বদা ১টি মাত্র ডিফল্ট "নগদ ক্যাশ" ওয়ালেট নিশ্চিত করা
        // এবং কোনো ডুপ্লিকেট ক্যাশ ওয়ালেট থাকলে তা স্বয়ংক্রিয়ভাবে ক্লিন করা (Deduplication)
        viewModelScope.launch {
            val existing = repository.allWallets.first()
            val allTx = repository.allTransactions.first()
            val usedMethods = allTx.mapNotNull { it.paymentMethod }.toSet()

            val cashWallets = existing.filter { it.name == "নগদ ক্যাশ" || it.accountType == "CASH" }

            if (cashWallets.isEmpty()) {
                repository.insertWallet(WalletEntity.createDefaultCashWallet())
            } else if (cashWallets.size > 1) {
                // একাধিক নগদ ক্যাশ ওয়ালেট থাকলে ১টি রেখে বাকিগুলো ডিলিট করা
                val toKeep = cashWallets.find { it.balance != 0.0 } ?: cashWallets.first()
                cashWallets.forEach { w ->
                    if (w.id != toKeep.id) {
                        repository.deleteWallet(w)
                        CloudSyncManager.getInstance().deleteItemFromCloud("wallets", w.id)
                    }
                }
                repository.setDefaultWallet(toKeep.id)
            } else {
                // ১টি ক্যাশ ওয়ালেট আছে; যদি কোনো ওয়ালেটই ডিফল্ট না থাকে অথবা একাধিক ডিফল্ট থাকে
                val defaultCount = existing.count { it.isDefault }
                if (defaultCount != 1) {
                    val targetDefault = existing.find { it.isDefault } ?: cashWallets.first()
                    repository.setDefaultWallet(targetDefault.id)
                }
            }

            // পূর্ববর্তী ডামি ওয়ালেট (বিকাশ, নগদ, ব্যাংক একাউন্ট) যেগুলোতে ব্যালেন্স ০ ও কোনো লেনদেন নেই তা পরিষ্কার করা
            val currentList = repository.allWallets.first()
            val dummyNames = setOf("বিকাশ", "নগদ", "ব্যাংক একাউন্ট")
            for (w in currentList) {
                if (w.name in dummyNames && w.balance == 0.0 && w.name !in usedMethods && !w.isDefault) {
                    repository.deleteWallet(w)
                }
            }

            // পূর্বে সেভ করা ট্রানজেকশনে যদি paymentMethod ফাঁকা বা "ক্যাশ" থাকে, তা "নগদ ক্যাশ" এ সিঙ্ক করা
            for (tx in allTx) {
                if (tx.paymentMethod.isNullOrBlank() || tx.paymentMethod == "ক্যাশ") {
                    repository.update(tx.copy(paymentMethod = "নগদ ক্যাশ"))
                }
            }
        }

        allReminders = repository.allReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        allQuickEntries = repository.allQuickEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allPersons = repository.allPersons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        deletedPersons = repository.deletedPersons.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allPersonTransactions = repository.allPersonTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allMarketLists = repository.allMarketLists.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBudgets = repository.allBudgets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSavingsGoals = repository.allSavingsGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTaskItems = repository.allTaskItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insert(transaction: TransactionEntity) = viewModelScope.launch {
        val cleanPayment = if (transaction.paymentMethod.isNullOrBlank() || transaction.paymentMethod == "ক্যাশ") {
            "নগদ ক্যাশ"
        } else {
            transaction.paymentMethod
        }
        repository.insert(transaction.copy(paymentMethod = cleanPayment))
    }

    fun update(transaction: TransactionEntity) = viewModelScope.launch {
        val cleanPayment = if (transaction.paymentMethod.isNullOrBlank() || transaction.paymentMethod == "ক্যাশ") {
            "নগদ ক্যাশ"
        } else {
            transaction.paymentMethod
        }
        repository.update(transaction.copy(paymentMethod = cleanPayment))
    }

    fun delete(transaction: TransactionEntity) = viewModelScope.launch {
        repository.delete(transaction)
        CloudSyncManager.getInstance().deleteItemFromCloud("transactions", transaction.id)
    }

    fun insertQuickEntry(quickEntry: QuickEntryEntity) = viewModelScope.launch {
        repository.insertQuickEntry(quickEntry)
    }

    fun updateQuickEntry(quickEntry: QuickEntryEntity) = viewModelScope.launch {
        repository.updateQuickEntry(quickEntry)
    }

    fun deleteQuickEntry(quickEntry: QuickEntryEntity) = viewModelScope.launch {
        repository.deleteQuickEntry(quickEntry)
        CloudSyncManager.getInstance().deleteItemFromCloud("quick_entries", quickEntry.id)
    }

    fun updateQuickEntryOrder(orderedEntries: List<QuickEntryEntity>) = viewModelScope.launch {
        orderedEntries.forEachIndexed { index, entry ->
            if (entry.orderIndex != index) {
                repository.updateQuickEntry(entry.copy(orderIndex = index))
            }
        }
    }

    // Person Methods
    fun insertPerson(person: PersonEntity) = viewModelScope.launch {
        repository.insertPerson(person)
    }

    fun updatePerson(person: PersonEntity) = viewModelScope.launch {
        repository.updatePerson(person)
    }

    fun softDeletePerson(person: PersonEntity) = viewModelScope.launch {
        repository.updatePerson(person.copy(isDeleted = true))
    }

    fun restorePerson(person: PersonEntity) = viewModelScope.launch {
        repository.updatePerson(person.copy(isDeleted = false))
    }

    fun permanentlyDeletePerson(person: PersonEntity) = viewModelScope.launch {
        repository.deletePerson(person)
        CloudSyncManager.getInstance().deleteItemFromCloud("persons", person.id)
    }

    // PersonTransaction Methods
    fun insertPersonTransaction(transaction: PersonTransactionEntity) = viewModelScope.launch {
        repository.insertPersonTransaction(transaction)
    }

    fun updatePersonTransaction(transaction: PersonTransactionEntity) = viewModelScope.launch {
        repository.updatePersonTransaction(transaction)
    }

    fun deletePersonTransaction(transaction: PersonTransactionEntity) = viewModelScope.launch {
        repository.deletePersonTransaction(transaction)
        CloudSyncManager.getInstance().deleteItemFromCloud("person_transactions", transaction.id)
    }

    fun getTransactionsForPerson(personId: String): Flow<List<PersonTransactionEntity>> {
        return repository.getTransactionsForPerson(personId)
    }

    // Market List & Items Methods
    fun getMarketItems(listId: String): Flow<List<MarketItemEntity>> = repository.getMarketItems(listId)

    suspend fun getMarketItemsSync(listId: String): List<MarketItemEntity> = repository.getItemsForListSync(listId)

    fun insertMarketList(list: MarketListEntity) = viewModelScope.launch {
        repository.insertMarketList(list)
    }

    fun updateMarketList(list: MarketListEntity) = viewModelScope.launch {
        repository.updateMarketList(list)
        syncShoppingListExpense(list.id)
    }

    fun deleteMarketList(list: MarketListEntity) = viewModelScope.launch {
        if (list.linkedExpenseId != null) {
            val tx = repository.getTransactionById(list.linkedExpenseId)
            if (tx != null) {
                repository.delete(tx)
                CloudSyncManager.getInstance().deleteItemFromCloud("transactions", tx.id)
            }
        }
        val items = repository.getItemsForListSync(list.id)
        items.forEach { item ->
            CloudSyncManager.getInstance().deleteItemFromCloud("market_items", item.id)
        }
        repository.deleteMarketList(list)
        CloudSyncManager.getInstance().deleteItemFromCloud("market_lists", list.id)
    }

    fun insertMarketItem(item: MarketItemEntity) = viewModelScope.launch {
        repository.insertMarketItem(item)
        syncShoppingListExpense(item.listId)
    }

    fun updateMarketItem(item: MarketItemEntity) = viewModelScope.launch {
        repository.updateMarketItem(item)
        syncShoppingListExpense(item.listId)
    }

    fun deleteMarketItem(item: MarketItemEntity) = viewModelScope.launch {
        repository.deleteMarketItem(item)
        CloudSyncManager.getInstance().deleteItemFromCloud("market_items", item.id)
        syncShoppingListExpense(item.listId)
    }

    /**
     * বাজার শেষ করার পর স্বয়ংক্রিয়ভাবে মূল খরচে যোগ করা এবং ডুপ্লিকেট রোধ করা
     */
    fun completeShoppingList(
        list: MarketListEntity,
        paymentMethod: String,
        createExpense: Boolean = true,
        onComplete: () -> Unit = {}
    ) = viewModelScope.launch {
        val items = repository.getItemsForListSync(list.id)
        val purchasedItems = items.filter { it.isPurchased }
        val finalTotal = purchasedItems.sumOf { item ->
            val price = if (item.actualPrice > 0.0) item.actualPrice else item.estimatedPrice
            val qty = item.quantity.toEnglishDouble()
            price * (if (qty > 0.0) qty else 1.0)
        }

        var expenseId = list.linkedExpenseId

        if (createExpense && finalTotal > 0.0) {
            val existingTx = if (expenseId != null) repository.getTransactionById(expenseId) else null
            val now = Calendar.getInstance().time
            val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))
            val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))

            val titleText = if (list.shopName.isNotBlank()) "🛒 বাজার: ${list.title} (${list.shopName})" else "🛒 বাজার: ${list.title}"

            if (existingTx != null) {
                // পূর্বে খরচ তৈরি করা থাকলে আপডেট করা (Duplicate Protection)
                val updatedTx = existingTx.copy(
                    title = titleText,
                    amount = finalTotal,
                    paymentMethod = paymentMethod,
                    category = "বাজার"
                )
                repository.update(updatedTx)
            } else {
                // নতুন খরচ তৈরি
                val newTx = TransactionEntity(
                    title = titleText,
                    amount = finalTotal,
                    isIncome = false,
                    time = timeFormat.format(now),
                    date = list.date.ifBlank { dateFormat.format(now) },
                    category = "বাজার",
                    shoppingListId = list.id,
                    paymentMethod = paymentMethod
                )
                repository.insert(newTx)
                expenseId = newTx.id
            }
        }

        val completedDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
        val updatedList = list.copy(
            isCompleted = true,
            paymentMethod = paymentMethod,
            linkedExpenseId = expenseId,
            completedDate = completedDateFormat.format(Calendar.getInstance().time)
        )
        repository.updateMarketList(updatedList)
        onComplete()
    }

    /**
     * সম্পন্ন বাজারে কোনো পরিবর্তন হলে মূল খরচের টাকার পরিমাণ স্বয়ংক্রিয়ভাবে সিঙ্ক করা
     */
    fun syncShoppingListExpense(listId: String) = viewModelScope.launch {
        val list = repository.getMarketListById(listId) ?: return@launch
        if (!list.isCompleted || list.linkedExpenseId == null) return@launch

        val items = repository.getItemsForListSync(listId)
        val purchasedItems = items.filter { it.isPurchased }
        val newTotal = purchasedItems.sumOf { item ->
            val price = if (item.actualPrice > 0.0) item.actualPrice else item.estimatedPrice
            val qty = item.quantity.toEnglishDouble()
            price * (if (qty > 0.0) qty else 1.0)
        }

        val tx = repository.getTransactionById(list.linkedExpenseId)
            ?: repository.getTransactionByShoppingListId(listId)

        if (tx != null) {
            repository.update(tx.copy(amount = newTotal))
        }
    }

    /**
     * আগের বাজার তালিকা কপি করে নতুন বাজার তৈরি করা (Reuse Previous List)
     */
    fun reuseShoppingList(
        originalListId: String,
        newTitle: String,
        newBudget: Double,
        newDate: String,
        newShopName: String,
        onCreated: (MarketListEntity) -> Unit = {}
    ) = viewModelScope.launch {
        val items = repository.getItemsForListSync(originalListId)
        val newList = MarketListEntity(
            title = newTitle,
            date = newDate,
            budget = newBudget,
            shopName = newShopName,
            isCompleted = false,
            linkedExpenseId = null,
            completedDate = null
        )
        repository.insertMarketList(newList)

        items.forEach { oldItem ->
            val newItem = oldItem.copy(
                id = java.util.UUID.randomUUID().toString(),
                listId = newList.id,
                isPurchased = false,
                actualPrice = 0.0
            )
            repository.insertMarketItem(newItem)
        }

        onCreated(newList)
    }

    // Note Methods
    fun insertNote(note: NoteEntity) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: NoteEntity) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repository.deleteNote(note)
        CloudSyncManager.getInstance().deleteItemFromCloud("notes", note.id)
    }

    // Budget Methods
    fun insertBudget(budget: BudgetEntity) = viewModelScope.launch {
        repository.insertBudget(budget)
    }

    fun updateBudget(budget: BudgetEntity) = viewModelScope.launch {
        repository.updateBudget(budget)
    }

    fun deleteBudget(budget: BudgetEntity) = viewModelScope.launch {
        repository.deleteBudget(budget)
        CloudSyncManager.getInstance().deleteItemFromCloud("budgets", budget.id)
    }

    // Savings Goals Methods
    fun insertSavingsGoal(goal: SavingsGoalEntity) = viewModelScope.launch {
        repository.insertSavingsGoal(goal)
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) = viewModelScope.launch {
        repository.updateSavingsGoal(goal)
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) = viewModelScope.launch {
        repository.deleteSavingsGoal(goal)
        CloudSyncManager.getInstance().deleteItemFromCloud("savings_goals", goal.id)
    }

    // Savings Transaction Methods
    fun getSavingsTransactionsForGoal(goalId: String): kotlinx.coroutines.flow.Flow<List<SavingsTransactionEntity>> {
        return repository.getTransactionsForGoal(goalId)
    }

    fun insertSavingsTransaction(tx: SavingsTransactionEntity) = viewModelScope.launch {
        repository.insertSavingsTransaction(tx)
    }

    fun updateSavingsTransaction(tx: SavingsTransactionEntity) = viewModelScope.launch {
        repository.updateSavingsTransaction(tx)
    }

    fun deleteSavingsTransaction(tx: SavingsTransactionEntity) = viewModelScope.launch {
        repository.deleteSavingsTransaction(tx)
        CloudSyncManager.getInstance().deleteItemFromCloud("savings_transactions", tx.id)
    }

    // Task Item Methods
    fun insertTaskItem(task: TaskItemEntity) = viewModelScope.launch {
        repository.insertTaskItem(task)
    }

    fun updateTaskItem(task: TaskItemEntity) = viewModelScope.launch {
        repository.updateTaskItem(task)
    }

    fun deleteTaskItem(task: TaskItemEntity) = viewModelScope.launch {
        repository.deleteTaskItem(task)
        CloudSyncManager.getInstance().deleteItemFromCloud("tasks", task.id)
    }

    // Reminders Methods
    fun insertReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.insertReminder(reminder)
        ReminderScheduler.scheduleReminder(getApplication(), reminder)
    }

    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder)
        if (reminder.isEnabled && !reminder.isCompleted) {
            ReminderScheduler.scheduleReminder(getApplication(), reminder)
        } else {
            ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.deleteReminder(reminder)
        ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        CloudSyncManager.getInstance().deleteItemFromCloud("reminders", reminder.id)
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) = viewModelScope.launch {
        val updated = reminder.copy(isCompleted = !reminder.isCompleted)
        repository.updateReminder(updated)
        if (updated.isCompleted) {
            ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        } else if (updated.isEnabled) {
            ReminderScheduler.scheduleReminder(getApplication(), updated)
        }
    }

    fun toggleReminderEnabled(reminder: ReminderEntity) = viewModelScope.launch {
        val updated = reminder.copy(isEnabled = !reminder.isEnabled)
        repository.updateReminder(updated)
        if (updated.isEnabled && !updated.isCompleted) {
            ReminderScheduler.scheduleReminder(getApplication(), updated)
        } else {
            ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        }
    }

    // ==========================================
    // ওয়ালেট ও অ্যাকাউন্ট সংক্রান্ত মেথডসমূহ
    // ==========================================
    fun insertWallet(wallet: WalletEntity) = viewModelScope.launch {
        if (wallet.isDefault) {
            repository.setDefaultWallet(wallet.id)
        }
        repository.insertWallet(wallet)
    }

    fun updateWallet(wallet: WalletEntity) = viewModelScope.launch {
        if (wallet.isDefault) {
            repository.setDefaultWallet(wallet.id)
        }
        repository.updateWallet(wallet)
    }

    fun deleteWallet(wallet: WalletEntity) = viewModelScope.launch {
        repository.deleteWallet(wallet)
        CloudSyncManager.getInstance().deleteItemFromCloud("wallets", wallet.id)
        val remaining = repository.allWallets.first()
        if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            val nextDefault = remaining.find { it.name == "নগদ ক্যাশ" } ?: remaining.first()
            repository.setDefaultWallet(nextDefault.id)
        }
    }

    fun setDefaultWallet(walletId: String) = viewModelScope.launch {
        repository.setDefaultWallet(walletId)
    }

    fun transferBetweenWallets(fromWalletId: String, toWalletId: String, amount: Double, note: String) = viewModelScope.launch {
        if (fromWalletId != toWalletId && amount > 0) {
            val wallets = allWallets.value
            val fromWallet = wallets.find { it.id == fromWalletId }
            val toWallet = wallets.find { it.id == toWalletId }
            if (fromWallet != null && toWallet != null) {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))
                val dateText = dateFormat.format(calendar.time)
                val timeText = timeFormat.format(calendar.time)
                
                // ১. উৎস অ্যাকাউন্ট থেকে আউটফ্লো
                insert(
                    TransactionEntity(
                        title = if (note.isNotBlank()) "স্থানান্তর: $note" else "স্থানান্তর (${toWallet.name} এ)",
                        amount = amount,
                        isIncome = false,
                        time = timeText,
                        date = dateText,
                        category = "স্থানান্তর",
                        paymentMethod = fromWallet.name
                    )
                )
                // ২. গন্তব্য অ্যাকাউন্টে ইনফ্লো
                insert(
                    TransactionEntity(
                        title = if (note.isNotBlank()) "স্থানান্তর: $note" else "স্থানান্তর (${fromWallet.name} থেকে)",
                        amount = amount,
                        isIncome = true,
                        time = timeText,
                        date = dateText,
                        category = "স্থানান্তর",
                        paymentMethod = toWallet.name
                    )
                )
            }
        }
    }

    // Notification Operations
    fun insertNotification(notification: NotificationEntity) = viewModelScope.launch {
        repository.insertNotification(notification)
    }

    fun markNotificationAsRead(id: String) = viewModelScope.launch {
        repository.markNotificationAsRead(id)
    }

    fun markAllNotificationsAsRead() = viewModelScope.launch {
        repository.markAllNotificationsAsRead()
    }

    fun deleteNotification(id: String) = viewModelScope.launch {
        repository.deleteNotification(id)
    }

    fun clearAllNotifications() = viewModelScope.launch {
        repository.clearAllNotifications()
    }
}

