package com.planwell.app.domain.repository

import com.planwell.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeActiveRemindersForTask(taskId: Long): Flow<List<Reminder>>
    suspend fun getActiveRemindersForTask(taskId: Long): List<Reminder>
    suspend fun getAllActiveReminders(): List<Reminder>
    suspend fun getReminder(id: Long): Reminder?
    suspend fun scheduleReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun cancelReminder(id: Long)
    suspend fun cancelAllForTask(taskId: Long)
}
