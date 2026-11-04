package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opportunity_candidates",
    foreignKeys = [
        ForeignKey(
            entity = OpportunityEntity::class,
            parentColumns = ["id"],
            childColumns = ["opportunity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["opportunity_id"]),
        Index(value = ["kind"]),
    ],
)
data class OpportunityCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "opportunity_id") val opportunityId: Long,
    val kind: String,
    @ColumnInfo(name = "value_text") val valueText: String,
    @ColumnInfo(name = "source_snippet") val sourceSnippet: String? = null,
    @ColumnInfo(name = "is_rejected") val isRejected: Boolean = false,
    @ColumnInfo(name = "is_applied") val isApplied: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
)
