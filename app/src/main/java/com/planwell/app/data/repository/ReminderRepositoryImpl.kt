package com.planwell.app.data.repository

import com.planwell.app.data.alarm.ReminderScheduler
import com.planwell.app.data.local.dao.ReminderDao
import com.planwell.app.data.mapper.toDomain
import com.planwell.app.data.mapper.toEntity
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
) : ReminderRepository {

    override fun observeActiveRemindersForTask(taskId: Long): Flow<List<Reminder>> =
        reminderDao.observeActiveRemindersForTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveRemindersForTask(taskId: Long): List<Reminder> =
        reminderDao.getActiveRemindersForTask(taskId).map { it.toDomain() }

    override suspend fun getAllActiveReminders(): List<Reminder> =
        reminderDao.getAllActiveReminders().map { it.toDomain() }

    override suspend fun getReminder(id: Long): Reminder? =
        reminderDao.getReminderById(id)?.toDomain()

    override suspend fun scheduleReminder(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder.copy(id = 0, isActive = true).toEntity())
        val saved = reminder.copy(id = id, isActive = true)
        reminderScheduler.schedule(saved)
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder.toEntity())
        if (reminder.isActive) {
            reminderScheduler.schedule(reminder)
        } else {
            reminderScheduler.cancel(reminder.id)
        }
    }

    override suspend fun cancelReminder(id: Long) {
        reminderDao.cancelReminder(id)
        reminderScheduler.cancel(id)
    }

    override suspend fun cancelAllForTask(taskId: Long) {
        val active = reminderDao.getActiveRemindersForTask(taskId)
        reminderDao.cancelAllRemindersForTask(taskId)
        active.forEach { reminderScheduler.cancel(it.id) }
    }
}
