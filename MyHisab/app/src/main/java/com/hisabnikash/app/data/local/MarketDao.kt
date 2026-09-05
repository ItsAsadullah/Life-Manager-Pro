package com.hisabnikash.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {
    @Query("SELECT * FROM market_lists ORDER BY isCompleted ASC, date DESC")
    fun getAllMarketLists(): Flow<List<MarketListEntity>>

    @Query("SELECT * FROM market_items")
    fun getAllMarketItems(): Flow<List<MarketItemEntity>>

    @Query("SELECT * FROM market_items WHERE listId = :listId ORDER BY isPurchased ASC")
    fun getItemsForList(listId: String): Flow<List<MarketItemEntity>>

    @Query("SELECT * FROM market_lists WHERE id = :listId LIMIT 1")
    suspend fun getMarketListById(listId: String): MarketListEntity?

    @Query("SELECT * FROM market_items WHERE listId = :listId")
    suspend fun getItemsForListSync(listId: String): List<MarketItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketList(list: MarketListEntity)

    @Update
    suspend fun updateMarketList(list: MarketListEntity)

    @Delete
    suspend fun deleteMarketList(list: MarketListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItem(item: MarketItemEntity)

    @Update
    suspend fun updateMarketItem(item: MarketItemEntity)

    @Delete
    suspend fun deleteMarketItem(item: MarketItemEntity)

    @Query("DELETE FROM market_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: String)
}
