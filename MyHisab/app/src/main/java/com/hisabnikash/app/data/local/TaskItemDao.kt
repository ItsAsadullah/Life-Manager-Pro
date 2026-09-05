package com.hisabnikash.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskItemDao {
    @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, dateCreated DESC")
    fun getAllTaskItems(): Flow<List<TaskItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskItem(task: TaskItemEntity)

    @Update
    suspend fun updateTaskItem(task: TaskItemEntity)

    @Delete
    suspend fun deleteTaskItem(task: TaskItemEntity)
}
