package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "person_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class PersonTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val personId: String,
    val amount: Double,
    val isReceive: Boolean, // true = পাবো (I will receive / they owe me), false = দিবো (I will give / I owe them)
    val note: String = "",
    val date: String,
    val time: String
)
