package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.planwell.app.data.local.db.entity.OpportunityAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityAttachmentDao {
    @Query(
        """
        SELECT * FROM opportunity_attachments
        WHERE opportunity_id = :opportunityId
        ORDER BY created_at_ms ASC
        """,
    )
    fun observeForOpportunity(opportunityId: Long): Flow<List<OpportunityAttachmentEntity>>

    @Query(
        """
        SELECT * FROM opportunity_attachments
        WHERE opportunity_id = :opportunityId
        ORDER BY created_at_ms ASC
        """,
    )
    suspend fun getForOpportunity(opportunityId: Long): List<OpportunityAttachmentEntity>

    @Query("SELECT * FROM opportunity_attachments WHERE id = :id")
    suspend fun getById(id: Long): OpportunityAttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OpportunityAttachmentEntity): Long

    @Query("DELETE FROM opportunity_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM opportunity_attachments WHERE opportunity_id = :opportunityId")
    suspend fun deleteForOpportunity(opportunityId: Long)
}
