package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opportunity_attachments",
    foreignKeys = [
        ForeignKey(
            entity = OpportunityEntity::class,
            parentColumns = ["id"],
            childColumns = ["opportunity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["opportunity_id"])],
)
data class OpportunityAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "opportunity_id") val opportunityId: Long,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    val kind: String = "FILE",
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
)
