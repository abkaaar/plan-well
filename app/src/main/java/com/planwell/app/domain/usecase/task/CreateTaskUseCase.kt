package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task): Long {
        require(task.title.isNotBlank()) { "Title is required" }
        require(task.title.length <= 100) { "Title max 100 characters" }
        task.description?.let {
            require(it.length <= 500) { "Description max 500 characters" }
        }
        val now = System.currentTimeMillis()
        return repository.createTask(
            task.copy(
                title = task.title.trim(),
                description = task.description?.trim()?.ifBlank { null },
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
    }
}
