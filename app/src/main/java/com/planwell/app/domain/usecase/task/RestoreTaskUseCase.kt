package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.repository.TaskRepository
import javax.inject.Inject

class RestoreTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Long) {
        repository.restore(taskId)
    }
}
