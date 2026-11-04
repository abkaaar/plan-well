package com.planwell.app.domain.repository

import com.planwell.app.domain.model.ScheduledCall

interface CallRepository {
    suspend fun getById(id: Long): ScheduledCall?
    suspend fun getPending(nowMs: Long = System.currentTimeMillis()): List<ScheduledCall>
    suspend fun schedule(call: ScheduledCall): Long
    suspend fun update(call: ScheduledCall)
    suspend fun markCompleted(id: Long)
    suspend fun snooze(id: Long, newScheduledAtMs: Long)
    suspend fun cancel(id: Long)
}
