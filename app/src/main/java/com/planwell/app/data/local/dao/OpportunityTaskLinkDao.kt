package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.planwell.app.data.local.db.entity.OpportunityTaskLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityTaskLinkDao {
    @Query(
        """
        SELECT task_id FROM opportunity_task_links
        WHERE opportunity_id = :opportunityId
        ORDER BY task_id ASC
        """,
    )
    fun observeTaskIds(opportunityId: Long): Flow<List<Long>>

    @Query(
        """
        SELECT task_id FROM opportunity_task_links
        WHERE opportunity_id = :opportunityId
        ORDER BY task_id ASC
        """,
    )
    suspend fun getTaskIds(opportunityId: Long): List<Long>

    @Query(
        """
        SELECT opportunity_id FROM opportunity_task_links
        WHERE task_id = :taskId
        ORDER BY opportunity_id ASC
        """,
    )
    fun observeOpportunityIds(taskId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: OpportunityTaskLinkEntity)

    @Query(
        """
        DELETE FROM opportunity_task_links
        WHERE opportunity_id = :opportunityId AND task_id = :taskId
        """,
    )
    suspend fun delete(opportunityId: Long, taskId: Long)

    @Query("DELETE FROM opportunity_task_links WHERE opportunity_id = :opportunityId")
    suspend fun deleteForOpportunity(opportunityId: Long)
}
