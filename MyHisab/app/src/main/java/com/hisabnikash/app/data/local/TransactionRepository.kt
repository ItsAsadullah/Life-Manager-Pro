package com.hisabnikash.app.data.local

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val quickEntryDao: QuickEntryDao,
    private val personDao: PersonDao,
    private val personTransactionDao: PersonTransactionDao,
    private val marketDao: MarketDao,
    private val noteDao: NoteDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsTransactionDao: SavingsTransactionDao,
    private val taskItemDao: TaskItemDao,
    private val reminderDao: ReminderDao,
    private val walletDao: WalletDao,
    private val notificationDao: NotificationDao
) {
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getRecurringTransactions(): List<TransactionEntity> {
        return transactionDao.getRecurringTransactions()
    }

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun getTransactionByShoppingListId(shoppingListId: String): TransactionEntity? =
        transactionDao.getTransactionByShoppingListId(shoppingListId)

    suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    val allQuickEntries: Flow<List<QuickEntryEntity>> = quickEntryDao.getAllQuickEntries()

    suspend fun insertQuickEntry(quickEntry: QuickEntryEntity) {
        quickEntryDao.insertQuickEntry(quickEntry)
    }

    suspend fun updateQuickEntry(quickEntry: QuickEntryEntity) {
        quickEntryDao.updateQuickEntry(quickEntry)
    }

    suspend fun deleteQuickEntry(quickEntry: QuickEntryEntity) {
        quickEntryDao.deleteQuickEntry(quickEntry)
    }

    val allPersons: Flow<List<PersonEntity>> = personDao.getAllPersons()
    val deletedPersons: Flow<List<PersonEntity>> = personDao.getDeletedPersons()

    suspend fun insertPerson(person: PersonEntity) {
        personDao.insertPerson(person)
    }

    suspend fun updatePerson(person: PersonEntity) {
        personDao.updatePerson(person)
    }

    suspend fun deletePerson(person: PersonEntity) {
        personDao.deletePerson(person)
    }

    val allPersonTransactions: Flow<List<PersonTransactionEntity>> = personTransactionDao.getAllPersonTransactions()

    fun getTransactionsForPerson(personId: String): Flow<List<PersonTransactionEntity>> {
        return personTransactionDao.getTransactionsForPerson(personId)
    }

    suspend fun insertPersonTransaction(transaction: PersonTransactionEntity) {
        personTransactionDao.insertTransaction(transaction)
    }

    suspend fun updatePersonTransaction(transaction: PersonTransactionEntity) {
        personTransactionDao.updateTransaction(transaction)
    }

    suspend fun deletePersonTransaction(transaction: PersonTransactionEntity) {
        personTransactionDao.deleteTransaction(transaction)
    }

    // Market Lists & Items
    val allMarketLists: Flow<List<MarketListEntity>> = marketDao.getAllMarketLists()

    fun getMarketItems(listId: String): Flow<List<MarketItemEntity>> = marketDao.getItemsForList(listId)

    suspend fun insertMarketList(list: MarketListEntity) = marketDao.insertMarketList(list)
    suspend fun updateMarketList(list: MarketListEntity) = marketDao.updateMarketList(list)
    suspend fun deleteMarketList(list: MarketListEntity) = marketDao.deleteMarketList(list)
    suspend fun getMarketListById(listId: String): MarketListEntity? = marketDao.getMarketListById(listId)
    suspend fun getItemsForListSync(listId: String): List<MarketItemEntity> = marketDao.getItemsForListSync(listId)

    suspend fun insertMarketItem(item: MarketItemEntity) = marketDao.insertMarketItem(item)
    suspend fun updateMarketItem(item: MarketItemEntity) = marketDao.updateMarketItem(item)
    suspend fun deleteMarketItem(item: MarketItemEntity) = marketDao.deleteMarketItem(item)

    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insertNote(note: NoteEntity) = noteDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    // Budgets
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    // Savings Goals
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.insertSavingsGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.updateSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteSavingsGoal(goal)

    // Savings Transactions
    fun getTransactionsForGoal(goalId: String): Flow<List<SavingsTransactionEntity>> =
        savingsTransactionDao.getTransactionsForGoal(goalId)

    suspend fun insertSavingsTransaction(tx: SavingsTransactionEntity) = savingsTransactionDao.insertSavingsTransaction(tx)
    suspend fun updateSavingsTransaction(tx: SavingsTransactionEntity) = savingsTransactionDao.updateSavingsTransaction(tx)
    suspend fun deleteSavingsTransaction(tx: SavingsTransactionEntity) = savingsTransactionDao.deleteSavingsTransaction(tx)

    // Tasks
    val allTaskItems: Flow<List<TaskItemEntity>> = taskItemDao.getAllTaskItems()

    suspend fun insertTaskItem(task: TaskItemEntity) = taskItemDao.insertTaskItem(task)
    suspend fun updateTaskItem(task: TaskItemEntity) = taskItemDao.updateTaskItem(task)
    suspend fun deleteTaskItem(task: TaskItemEntity) = taskItemDao.deleteTaskItem(task)

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    suspend fun getActiveRemindersSync(): List<ReminderEntity> = reminderDao.getActiveRemindersSync()
    suspend fun getReminderById(id: String): ReminderEntity? = reminderDao.getReminderById(id)
    fun getRemindersForDate(date: String): Flow<List<ReminderEntity>> = reminderDao.getRemindersForDate(date)

    suspend fun insertReminder(reminder: ReminderEntity) = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)
    suspend fun updateReminderStatus(id: String, isCompleted: Boolean) = reminderDao.updateReminderStatus(id, isCompleted)
    suspend fun updateReminderEnabled(id: String, isEnabled: Boolean) = reminderDao.updateReminderEnabled(id, isEnabled)

    // Wallets
    suspend fun insertWallet(wallet: WalletEntity) = walletDao.insertWallet(wallet)
    suspend fun insertAllWallets(wallets: List<WalletEntity>) = walletDao.insertAll(wallets)
    suspend fun updateWallet(wallet: WalletEntity) = walletDao.updateWallet(wallet)
    suspend fun deleteWallet(wallet: WalletEntity) = walletDao.deleteWallet(wallet)
    suspend fun setDefaultWallet(id: String) = walletDao.setDefaultWallet(id)
    suspend fun adjustWalletBalance(id: String, delta: Double) = walletDao.adjustBalance(id, delta)

    // Notifications
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadNotificationCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insertNotification(notification: NotificationEntity) = notificationDao.insertNotification(notification)
    suspend fun insertNotifications(notifications: List<NotificationEntity>) = notificationDao.insertNotifications(notifications)
    suspend fun markNotificationAsRead(id: String) = notificationDao.markAsRead(id)
    suspend fun markAllNotificationsAsRead() = notificationDao.markAllAsRead()
    suspend fun deleteNotification(id: String) = notificationDao.deleteNotification(id)
    suspend fun clearAllNotifications() = notificationDao.clearAllNotifications()
    suspend fun hasNotificationForDayAndType(type: String, startOfDay: Long, endOfDay: Long): Boolean =
        notificationDao.hasNotificationForDayAndType(type, startOfDay, endOfDay) > 0
}

