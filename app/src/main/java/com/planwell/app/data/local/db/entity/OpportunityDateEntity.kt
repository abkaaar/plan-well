package com.planwell.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opportunity_dates",
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
        Index(value = ["type"]),
    ],
)
data class OpportunityDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "opportunity_id") val opportunityId: Long,
    val type: String,
    val label: String? = null,
    @ColumnInfo(name = "date_ms") val dateMs: Long? = null,
    @ColumnInfo(name = "extracted_date_ms") val extractedDateMs: Long? = null,
    @ColumnInfo(name = "is_user_confirmed") val isUserConfirmed: Boolean = false,
)
