package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "market_items",
    foreignKeys = [
        ForeignKey(
            entity = MarketListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class MarketItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String,
    val quantity: String = "১",
    val unit: String = "কেজি",
    val estimatedPrice: Double = 0.0,
    val actualPrice: Double = 0.0,
    val isPurchased: Boolean = false,
    val category: String = "সাধারণ",
    val note: String = "" // পণ্যের বিশেষ নোট (যেমন: তাজা দেখে আনা)
)
