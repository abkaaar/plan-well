package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_id")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "trigger_at_ms") val triggerAtMs: Long,
    @ColumnInfo(name = "repeat_type") val repeatType: String = "NONE",
    @ColumnInfo(name = "repeat_interval_ms") val repeatIntervalMs: Long? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)
