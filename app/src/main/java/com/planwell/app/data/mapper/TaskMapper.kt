package com.planwell.app.data.mapper

import com.planwell.app.data.local.db.entity.TaskEntity
import com.planwell.app.domain.model.Priority
import com.planwell.app.domain.model.Task

fun TaskEntity.toDomain(): Task =
    Task(
        id = id,
        title = title,
        description = description,
        dueDateMs = dueDateMs,
        priority = Priority.fromInt(priority),
        categoryId = categoryId,
        isCompleted = isCompleted,
        completedAtMs = completedAtMs,
        isDeleted = isDeleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )

fun Task.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDateMs = dueDateMs,
        priority = priority.value,
        categoryId = categoryId,
        isCompleted = isCompleted,
        completedAtMs = completedAtMs,
        isDeleted = isDeleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
