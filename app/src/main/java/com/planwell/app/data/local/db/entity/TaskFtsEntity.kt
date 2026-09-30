package com.planwell.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * External-content FTS mirror of [TaskEntity] title/description.
 * Room keeps it in sync via triggers when [TaskEntity] rows change.
 *
 * Uses FTS4 (Room’s contentEntity path). Match behaviour still satisfies
 * local full-text search (SRS FR-04); SQLite FTS5 tokenizer quirks avoided.
 */
@Entity(tableName = "tasks_fts")
@Fts4(contentEntity = TaskEntity::class)
data class TaskFtsEntity(
    val title: String,
    val description: String?,
)
