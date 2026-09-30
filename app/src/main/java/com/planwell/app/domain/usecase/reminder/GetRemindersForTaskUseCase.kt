package com.planwell.app.domain.usecase.reminder

import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRemindersForTaskUseCase @Inject constructor(
    private val repository: ReminderRepository,
) {
    operator fun invoke(taskId: Long): Flow<List<Reminder>> =
        repository.observeActiveRemindersForTask(taskId)
}
