package com.planwell.app.data.mapper

import com.planwell.app.data.local.db.entity.CallEntity
import com.planwell.app.domain.model.ScheduledCall

fun CallEntity.toDomain(): ScheduledCall =
    ScheduledCall(
        id = id,
        contactId = contactId,
        displayName = displayName,
        phoneNumber = phoneNumber,
        scheduledAtMs = scheduledAtMs,
        notes = notes,
        isCompleted = isCompleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )

fun ScheduledCall.toEntity(): CallEntity =
    CallEntity(
        id = id,
        contactId = contactId,
        displayName = displayName,
        phoneNumber = phoneNumber,
        scheduledAtMs = scheduledAtMs,
        notes = notes,
        isCompleted = isCompleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
