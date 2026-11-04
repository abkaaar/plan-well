package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.OpportunityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDao {
    @Query(
        """
        SELECT * FROM opportunities
        WHERE is_deleted = 0 AND status != 'ARCHIVED'
        ORDER BY updated_at_ms DESC
        """,
    )
    fun observeInbox(): Flow<List<OpportunityEntity>>

    @Query(
        """
        SELECT * FROM opportunities
        WHERE is_deleted = 0 AND status = 'ARCHIVED'
        ORDER BY updated_at_ms DESC
        """,
    )
    fun observeArchived(): Flow<List<OpportunityEntity>>

    @Query(
        """
        SELECT * FROM opportunities
        WHERE is_deleted = 1
        ORDER BY updated_at_ms DESC
        """,
    )
    fun observeDeleted(): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE id = :id")
    suspend fun getById(id: Long): OpportunityEntity?

    @Query("SELECT * FROM opportunities WHERE id = :id")
    fun observeById(id: Long): Flow<OpportunityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OpportunityEntity): Long

    @Update
    suspend fun update(entity: OpportunityEntity)

    @Query(
        """
        UPDATE opportunities
        SET is_deleted = 1, updated_at_ms = :now
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE opportunities
        SET is_deleted = 0, updated_at_ms = :now
        WHERE id = :id
        """,
    )
    suspend fun restore(id: Long, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE opportunities
        SET status = :status, updated_at_ms = :now
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM opportunities WHERE id = :id")
    suspend fun permanentlyDelete(id: Long)
}
