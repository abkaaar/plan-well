package com.planwell.app.domain.repository

import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeActiveTasks(): Flow<List<Task>>
    fun observeDeletedTasks(): Flow<List<Task>>
    fun observeTask(id: Long): Flow<Task?>
    fun searchTasks(ftsQuery: String): Flow<List<Task>>
    fun observeCompletionStats(startMs: Long, endMs: Long): Flow<CompletionStats>
    suspend fun getTask(id: Long): Task?
    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun permanentlyDelete(id: Long)
    suspend fun completeTask(id: Long)
    suspend fun uncompleteTask(id: Long)
}
