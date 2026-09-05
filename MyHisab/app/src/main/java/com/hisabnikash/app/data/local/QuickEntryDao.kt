package com.hisabnikash.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickEntryDao {
    @Query("SELECT * FROM quick_entries ORDER BY orderIndex ASC, rowid DESC")
    fun getAllQuickEntries(): Flow<List<QuickEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickEntry(quickEntry: QuickEntryEntity)

    @Update
    suspend fun updateQuickEntry(quickEntry: QuickEntryEntity)

    @Delete
    suspend fun deleteQuickEntry(quickEntry: QuickEntryEntity)
}
