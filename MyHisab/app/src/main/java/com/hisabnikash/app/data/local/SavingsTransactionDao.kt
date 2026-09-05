package com.hisabnikash.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsTransactionDao {
    @Query("SELECT * FROM savings_transactions WHERE goalId = :goalId ORDER BY date DESC")
    fun getTransactionsForGoal(goalId: String): Flow<List<SavingsTransactionEntity>>

    @Query("SELECT * FROM savings_transactions")
    fun getAllSavingsTransactions(): Flow<List<SavingsTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsTransaction(transaction: SavingsTransactionEntity)

    @Update
    suspend fun updateSavingsTransaction(transaction: SavingsTransactionEntity)

    @Delete
    suspend fun deleteSavingsTransaction(transaction: SavingsTransactionEntity)
}
