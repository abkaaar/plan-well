package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Long, completed: Boolean = true) {
        if (completed) {
            repository.completeTask(taskId)
        } else {
            repository.uncompleteTask(taskId)
        }
    }
}
