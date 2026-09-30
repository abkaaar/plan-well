package com.planwell.app.data.mapper

import com.planwell.app.data.local.db.entity.ReminderEntity
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.RepeatType

fun ReminderEntity.toDomain(): Reminder =
    Reminder(
        id = id,
        taskId = taskId,
        triggerAtMs = triggerAtMs,
        repeatType = runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE),
        repeatIntervalMs = repeatIntervalMs,
        isActive = isActive,
    )

fun Reminder.toEntity(): ReminderEntity =
    ReminderEntity(
        id = id,
        taskId = taskId,
        triggerAtMs = triggerAtMs,
        repeatType = repeatType.name,
        repeatIntervalMs = repeatIntervalMs,
        isActive = isActive,
    )
