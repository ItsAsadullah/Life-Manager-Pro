package com.hisabnikash.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonTransactionDao {
    @Query("SELECT * FROM person_transactions WHERE personId = :personId ORDER BY date DESC, time DESC")
    fun getTransactionsForPerson(personId: String): Flow<List<PersonTransactionEntity>>
    
    @Query("SELECT * FROM person_transactions")
    fun getAllPersonTransactions(): Flow<List<PersonTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PersonTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: PersonTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: PersonTransactionEntity)
}
