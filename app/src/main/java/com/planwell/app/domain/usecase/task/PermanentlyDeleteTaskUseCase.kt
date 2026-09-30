package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.repository.TaskRepository
import javax.inject.Inject

class PermanentlyDeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Long) {
        repository.permanentlyDelete(taskId)
    }
}
