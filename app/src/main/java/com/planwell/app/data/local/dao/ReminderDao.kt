package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE task_id = :taskId AND is_active = 1")
    suspend fun getActiveRemindersForTask(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE task_id = :taskId AND is_active = 1")
    fun observeActiveRemindersForTask(taskId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE is_active = 1")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET is_active = 0 WHERE id = :id")
    suspend fun cancelReminder(id: Long)

    @Query("UPDATE reminders SET is_active = 0 WHERE task_id = :taskId")
    suspend fun cancelAllRemindersForTask(taskId: Long)
}
