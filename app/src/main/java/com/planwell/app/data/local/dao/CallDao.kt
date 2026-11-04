package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.CallEntity

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallEntity): Long

    @Update
    suspend fun update(call: CallEntity)

    @Query("SELECT * FROM scheduled_calls WHERE id = :id")
    suspend fun getById(id: Long): CallEntity?

    @Query(
        """
        SELECT * FROM scheduled_calls
        WHERE is_completed = 0 AND scheduled_at_ms > :nowMs
        ORDER BY scheduled_at_ms ASC
        """,
    )
    suspend fun getPending(nowMs: Long): List<CallEntity>

    @Query(
        """
        UPDATE scheduled_calls
        SET is_completed = 1, updated_at_ms = :updatedAtMs
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(id: Long, updatedAtMs: Long)

    @Query("DELETE FROM scheduled_calls WHERE id = :id")
    suspend fun delete(id: Long)
}
