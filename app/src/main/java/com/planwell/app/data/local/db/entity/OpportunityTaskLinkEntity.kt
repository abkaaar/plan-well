package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "opportunity_task_links",
    primaryKeys = ["opportunity_id", "task_id"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunityEntity::class,
            parentColumns = ["id"],
            childColumns = ["opportunity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["opportunity_id"]),
        Index(value = ["task_id"]),
    ],
)
data class OpportunityTaskLinkEntity(
    @ColumnInfo(name = "opportunity_id") val opportunityId: Long,
    @ColumnInfo(name = "task_id") val taskId: Long,
)
