package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val time: String,
    val date: String,
    val category: String,
    val isRecurring: Boolean = false,
    val recurringPeriod: String? = null, // "Daily", "Weekly", "Monthly", "Yearly"
    val shoppingListId: String? = null,  // বাজার লিস্টের সাথে সরাসরি সংযোগ
    val paymentMethod: String? = null    // ক্যাশ, বিকাশ, নগদ, ব্যাংক, কার্ড ইত্যাদি
)
