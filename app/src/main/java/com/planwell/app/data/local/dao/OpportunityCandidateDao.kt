package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.OpportunityCandidateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityCandidateDao {
    @Query(
        """
        SELECT * FROM opportunity_candidates
        WHERE opportunity_id = :opportunityId
        ORDER BY id ASC
        """,
    )
    fun observeForOpportunity(opportunityId: Long): Flow<List<OpportunityCandidateEntity>>

    @Query(
        """
        SELECT * FROM opportunity_candidates
        WHERE opportunity_id = :opportunityId
        ORDER BY id ASC
        """,
    )
    suspend fun getForOpportunity(opportunityId: Long): List<OpportunityCandidateEntity>

    @Query("SELECT * FROM opportunity_candidates WHERE id = :id")
    suspend fun getById(id: Long): OpportunityCandidateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OpportunityCandidateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OpportunityCandidateEntity>)

    @Update
    suspend fun update(entity: OpportunityCandidateEntity)

    @Query("DELETE FROM opportunity_candidates WHERE opportunity_id = :opportunityId")
    suspend fun deleteForOpportunity(opportunityId: Long)

    @Query(
        """
        DELETE FROM opportunity_candidates
        WHERE opportunity_id = :opportunityId
          AND is_applied = 0
          AND is_rejected = 0
        """,
    )
    suspend fun deletePendingForOpportunity(opportunityId: Long)
}
