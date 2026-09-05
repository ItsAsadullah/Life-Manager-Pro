package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * রিমাইন্ডার ও ক্যালেন্ডার ইভেন্ট সত্ত্বা
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val eventDate: String,                 // যেমন: "০৪ সেপ, ২০২৬"
    val eventTime: String,                 // যেমন: "১০:৩০ AM"
    val timestamp: Long,                   // মূল ইভেন্টের মিলিসেকেন্ড
    val triggerTimestamp: Long,            // অগ্রিম নোটিশ সমন্বয় করে অ্যালার্ম ট্রিগারের মিলিসেকেন্ড
    val advanceNotice: String = "নির্দিষ্ট সময়ে", // "নির্দিষ্ট সময়ে", "১৫ মিনিট আগে", "১ দিন আগে" ইত্যাদি
    val advanceNoticeMinutes: Long = 0L,   // ০, ১৫, ৩০, ৬০, ১৪৪০ ইত্যাদি
    val alertType: String = "NOTIFICATION",// "NOTIFICATION" অথবা "ALARM"
    val category: String = "ব্যক্তিগত 👤",   // "ব্যক্তিগত 👤", "বিল 💳", "অফিস 💼", "ঔষধ 💊", "বার্ষিকী 🎂", "অন্যান্য 📦"
    val priority: String = "MEDIUM",       // "LOW", "MEDIUM", "HIGH"
    val repeatInterval: String = "NONE",   // "NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val isCompleted: Boolean = false,      // সম্পন্ন কিনা
    val isEnabled: Boolean = true,         // অ্যালার্ম সক্রিয় কিনা
    val dateCreated: Long = System.currentTimeMillis()
)
