package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,                           // অ্যাকাউন্টের নাম (যেমন: নগদ ক্যাশ, বিকাশ পার্সোনাল, ব্র্যাক ব্যাংক)
    val accountType: String = "CASH",           // CASH, BKASH, NAGAD, ROCKET, UPAY, BANK, CARD, OTHER
    val accountNumber: String = "",             // অ্যাকাউন্ট নম্বর (ঐচ্ছিক)
    val balance: Double = 0.0,                  // বর্তমান বা প্রারম্ভিক ব্যালেন্স
    val colorHex: Long = 0xFF0A84FF,            // অ্যাকাউন্টের কালার থিম
    val isDefault: Boolean = false,             // ডিফল্ট ওয়ালেট কিনা
    val notes: String = "",                     // অতিরিক্ত নোট
    val orderIndex: Int = 0                     // প্রদর্শনের ক্রম
)
