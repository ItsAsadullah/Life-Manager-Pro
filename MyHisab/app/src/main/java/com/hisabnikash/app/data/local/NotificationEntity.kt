package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // REMINDER, DAILY_MORNING, DAILY_AFTERNOON, DAILY_NIGHT, BUDGET_ALERT, RECURRING_TRANSACTION, SYSTEM
    val isRead: Boolean = false,
    val actionRoute: String? = null, // reminders, budget, dashboard, market, etc.
    val referenceId: String? = null,
    val priority: String = "NORMAL" // HIGH, NORMAL, LOW
)
