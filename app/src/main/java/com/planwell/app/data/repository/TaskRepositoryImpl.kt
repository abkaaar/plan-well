package com.planwell.app.data.repository

import com.planwell.app.data.local.dao.TaskDao
import com.planwell.app.data.local.dao.TaskFtsDao
import com.planwell.app.data.mapper.toDomain
import com.planwell.app.data.mapper.toEntity
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskFtsDao: TaskFtsDao,
) : TaskRepository {

    override fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.getActiveTasks().map { list -> list.map { it.toDomain() } }

    override fun observeDeletedTasks(): Flow<List<Task>> =
        taskDao.getDeletedTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTask(id: Long): Flow<Task?> =
        taskDao.observeTaskById(id).map { it?.toDomain() }

    override fun searchTasks(ftsQuery: String): Flow<List<Task>> =
        taskFtsDao.search(ftsQuery).map { list -> list.map { it.toDomain() } }

    override fun observeCompletionStats(startMs: Long, endMs: Long): Flow<CompletionStats> =
        combine(
            taskDao.observeTodayTotal(startMs, endMs),
            taskDao.observeTodayCompleted(startMs, endMs),
        ) { total, completed ->
            CompletionStats(todayTotal = total, todayCompleted = completed)
        }

    override suspend fun getTask(id: Long): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun createTask(task: Task): Long =
        taskDao.insertTask(task.copy(id = 0).toEntity())

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(
            task.copy(updatedAtMs = System.currentTimeMillis()).toEntity(),
        )
    }

    override suspend fun softDelete(id: Long) {
        taskDao.softDelete(id)
    }

    override suspend fun restore(id: Long) {
        taskDao.restore(id)
    }

    override suspend fun permanentlyDelete(id: Long) {
        val entity = taskDao.getTaskById(id) ?: return
        taskDao.permanentlyDelete(entity)
    }

    override suspend fun completeTask(id: Long) {
        taskDao.markComplete(id)
    }

    override suspend fun uncompleteTask(id: Long) {
        taskDao.markIncomplete(id)
    }
}
