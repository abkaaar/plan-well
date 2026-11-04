package com.planwell.app.data.repository

import com.planwell.app.data.alarm.ReminderScheduler
import com.planwell.app.data.local.dao.CallDao
import com.planwell.app.data.mapper.toDomain
import com.planwell.app.data.mapper.toEntity
import com.planwell.app.domain.model.ScheduledCall
import com.planwell.app.domain.repository.CallRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val callDao: CallDao,
    private val reminderScheduler: ReminderScheduler,
) : CallRepository {

    override suspend fun getById(id: Long): ScheduledCall? =
        callDao.getById(id)?.toDomain()

    override suspend fun getPending(nowMs: Long): List<ScheduledCall> =
        callDao.getPending(nowMs).map { it.toDomain() }

    override suspend fun schedule(call: ScheduledCall): Long {
        val now = System.currentTimeMillis()
        val toInsert = call.copy(
            id = 0,
            isCompleted = false,
            createdAtMs = if (call.createdAtMs > 0) call.createdAtMs else now,
            updatedAtMs = now,
        )
        val id = callDao.insert(toInsert.toEntity())
        val saved = toInsert.copy(id = id)
        reminderScheduler.scheduleCall(saved)
        return id
    }

    override suspend fun update(call: ScheduledCall) {
        val now = System.currentTimeMillis()
        val updated = call.copy(updatedAtMs = now)
        callDao.update(updated.toEntity())
        if (updated.isCompleted) {
            reminderScheduler.cancelCall(updated.id)
        } else {
            reminderScheduler.scheduleCall(updated)
        }
    }

    override suspend fun markCompleted(id: Long) {
        reminderScheduler.cancelCall(id)
        callDao.markCompleted(id, System.currentTimeMillis())
    }

    override suspend fun snooze(id: Long, newScheduledAtMs: Long) {
        val existing = callDao.getById(id) ?: return
        val snoozed = existing.copy(
            scheduledAtMs = newScheduledAtMs,
            isCompleted = false,
            updatedAtMs = System.currentTimeMillis(),
        )
        callDao.update(snoozed)
        reminderScheduler.scheduleCall(snoozed.toDomain())
    }

    override suspend fun cancel(id: Long) {
        reminderScheduler.cancelCall(id)
        callDao.delete(id)
    }
}
