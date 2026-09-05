package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "market_lists")
data class MarketListEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val budget: Double = 0.0,
    val isCompleted: Boolean = false,
    val category: String = "বাজার",
    val shopName: String = "",           // দোকানের নাম (যেমন: স্বপ্ন, কাঁচাবাজার, মুদি)
    val paymentMethod: String = "ক্যাশ", // পরিশোধের মাধ্যম
    val linkedExpenseId: String? = null, // মূল খরচে যুক্ত ট্রানজ্যাকশন আইডি
    val completedDate: String? = null    // বাজার সম্পন্ন করার তারিখ
)
