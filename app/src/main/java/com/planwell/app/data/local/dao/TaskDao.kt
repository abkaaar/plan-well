package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0
        ORDER BY is_completed ASC, due_date_ms ASC, priority DESC
        """,
    )
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 1
        ORDER BY updated_at_ms DESC
        """,
    )
    fun getDeletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: Long): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query(
        """
        UPDATE tasks
        SET is_deleted = 1, updated_at_ms = :now
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE tasks
        SET is_deleted = 0, updated_at_ms = :now
        WHERE id = :id
        """,
    )
    suspend fun restore(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun permanentlyDelete(task: TaskEntity)

    @Query(
        """
        UPDATE tasks
        SET is_completed = 1, completed_at_ms = :time, updated_at_ms = :time
        WHERE id = :id
        """,
    )
    suspend fun markComplete(id: Long, time: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE tasks
        SET is_completed = 0, completed_at_ms = NULL, updated_at_ms = :time
        WHERE id = :id
        """,
    )
    suspend fun markIncomplete(id: Long, time: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0
          AND due_date_ms BETWEEN :startMs AND :endMs
        ORDER BY is_completed ASC, due_date_ms ASC, priority DESC
        """,
    )
    fun getTasksForDay(startMs: Long, endMs: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE is_deleted = 0
          AND due_date_ms BETWEEN :startMs AND :endMs
        """,
    )
    fun observeTodayTotal(startMs: Long, endMs: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE is_deleted = 0
          AND is_completed = 1
          AND due_date_ms BETWEEN :startMs AND :endMs
        """,
    )
    fun observeTodayCompleted(startMs: Long, endMs: Long): Flow<Int>
}
