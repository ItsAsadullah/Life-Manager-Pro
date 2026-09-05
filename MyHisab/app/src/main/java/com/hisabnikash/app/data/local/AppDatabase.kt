package com.hisabnikash.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        QuickEntryEntity::class,
        PersonEntity::class,
        PersonTransactionEntity::class,
        MarketListEntity::class,
        MarketItemEntity::class,
        NoteEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        SavingsTransactionEntity::class,
        TaskItemEntity::class,
        ReminderEntity::class,
        WalletEntity::class,
        NotificationEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun quickEntryDao(): QuickEntryDao
    abstract fun personDao(): PersonDao
    abstract fun personTransactionDao(): PersonTransactionDao
    abstract fun marketDao(): MarketDao
    abstract fun noteDao(): NoteDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsTransactionDao(): SavingsTransactionDao
    abstract fun taskItemDao(): TaskItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun walletDao(): WalletDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quick_entries` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `isIncome` INTEGER NOT NULL, `category` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `persons` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `dateAdded` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `person_transactions` (`id` TEXT NOT NULL, `personId` TEXT NOT NULL, `amount` REAL NOT NULL, `isReceive` INTEGER NOT NULL, `note` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_person_transactions_personId` ON `person_transactions` (`personId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `persons` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `persons` ADD COLUMN `photoUri` TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `market_lists` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `date` TEXT NOT NULL, `budget` REAL NOT NULL, `isCompleted` INTEGER NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `market_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` TEXT NOT NULL, `unit` TEXT NOT NULL, `estimatedPrice` REAL NOT NULL, `actualPrice` REAL NOT NULL, `isPurchased` INTEGER NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `market_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_market_items_listId` ON `market_items` (`listId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `dateCreated` TEXT NOT NULL, `dateUpdated` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, `monthYear` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings_goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `targetDate` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `priority` TEXT NOT NULL, `dateCreated` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings_transactions` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `amount` REAL NOT NULL, `isDeposit` INTEGER NOT NULL, `note` TEXT NOT NULL, `date` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_savings_transactions_goalId` ON `savings_transactions` (`goalId`)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `shoppingListId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentMethod` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `market_lists` ADD COLUMN `shopName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `market_lists` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'ক্যাশ'")
                db.execSQL("ALTER TABLE `market_lists` ADD COLUMN `linkedExpenseId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `market_lists` ADD COLUMN `completedDate` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `market_items` ADD COLUMN `note` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `eventDate` TEXT NOT NULL, `eventTime` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `triggerTimestamp` INTEGER NOT NULL, `advanceNotice` TEXT NOT NULL, `advanceNoticeMinutes` INTEGER NOT NULL, `alertType` TEXT NOT NULL, `category` TEXT NOT NULL, `priority` TEXT NOT NULL, `repeatInterval` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `dateCreated` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wallets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `accountType` TEXT NOT NULL, `accountNumber` TEXT NOT NULL, `balance` REAL NOT NULL, `colorHex` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, `notes` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notifications` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `type` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `actionRoute` TEXT, `referenceId` TEXT, `priority` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisabnikash_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
