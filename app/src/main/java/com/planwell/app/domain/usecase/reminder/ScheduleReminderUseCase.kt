package com.planwell.app.domain.usecase.reminder

import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.repository.ReminderRepository
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        require(reminder.taskId > 0) { "Task id required" }
        require(reminder.triggerAtMs > System.currentTimeMillis()) {
            "Reminder time must be in the future"
        }
        return repository.scheduleReminder(reminder)
    }
}
