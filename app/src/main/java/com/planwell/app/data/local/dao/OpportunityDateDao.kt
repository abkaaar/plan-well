package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.OpportunityDateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDateDao {
    @Query(
        """
        SELECT * FROM opportunity_dates
        WHERE opportunity_id = :opportunityId
        ORDER BY type ASC, id ASC
        """,
    )
    fun observeForOpportunity(opportunityId: Long): Flow<List<OpportunityDateEntity>>

    @Query(
        """
        SELECT * FROM opportunity_dates
        WHERE opportunity_id = :opportunityId
        ORDER BY type ASC, id ASC
        """,
    )
    suspend fun getForOpportunity(opportunityId: Long): List<OpportunityDateEntity>

    @Query("SELECT * FROM opportunity_dates WHERE id = :id")
    suspend fun getById(id: Long): OpportunityDateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OpportunityDateEntity): Long

    @Update
    suspend fun update(entity: OpportunityDateEntity)

    @Query("DELETE FROM opportunity_dates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM opportunity_dates WHERE opportunity_id = :opportunityId")
    suspend fun deleteForOpportunity(opportunityId: Long)
}
