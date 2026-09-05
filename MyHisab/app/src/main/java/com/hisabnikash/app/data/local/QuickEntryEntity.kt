package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quick_entries")
data class QuickEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String, // Can be note or name
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val orderIndex: Int = 0 // New field for custom sorting
)
