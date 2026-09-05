package com.hisabnikash.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "task_items")
data class TaskItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "সাধারণ", // "জরুরী", "সাধারণ", "কম"
    val dateCreated: String = ""
)
