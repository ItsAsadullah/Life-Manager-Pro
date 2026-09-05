package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "সাধারণ",
    val colorHex: String = "#1E293B",
    val isPinned: Boolean = false,
    val dateCreated: String,
    val dateUpdated: String
)
