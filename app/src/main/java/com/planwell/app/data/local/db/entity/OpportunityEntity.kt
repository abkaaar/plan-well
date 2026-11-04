package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opportunities",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["status"]),
        Index(value = ["is_deleted"]),
    ],
)
data class OpportunityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val type: String = "OPPORTUNITY",
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val status: String = "NEEDS_REVIEW",
    @ColumnInfo(name = "source_url") val sourceUrl: String? = null,
    @ColumnInfo(name = "source_platform") val sourcePlatform: String = "UNKNOWN",
    @ColumnInfo(name = "capture_method") val captureMethod: String = "UNKNOWN",
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "extraction_status") val extractionStatus: String = "NONE",
    @ColumnInfo(name = "extraction_error") val extractionError: String? = null,
    @ColumnInfo(name = "extracted_raw_text") val extractedRawText: String? = null,
    @ColumnInfo(name = "last_extraction_at_ms") val lastExtractionAtMs: Long? = null,
    @ColumnInfo(name = "extraction_engine") val extractionEngine: String? = null,
)
