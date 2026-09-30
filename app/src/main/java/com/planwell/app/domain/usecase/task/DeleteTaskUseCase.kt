package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.repository.ReminderRepository
import com.planwell.app.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminderRepository: ReminderRepository,
) {
    suspend operator fun invoke(taskId: Long) {
        reminderRepository.cancelAllForTask(taskId)
        repository.softDelete(taskId)
    }
}
