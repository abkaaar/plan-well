package com.planwell.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.planwell.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFtsDao {
    @Query(
        """
        SELECT tasks.* FROM tasks
        JOIN tasks_fts ON tasks.id = tasks_fts.rowid
        WHERE tasks_fts MATCH :query
          AND tasks.is_deleted = 0
        ORDER BY tasks.is_completed ASC, tasks.due_date_ms ASC, tasks.priority DESC
        """,
    )
    fun search(query: String): Flow<List<TaskEntity>>
}
