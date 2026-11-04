package com.planwell.app.domain.usecase.reminder

import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.repository.ReminderRepository
import javax.inject.Inject
import kotlin.math.abs

class ScheduleReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
) {
    /**
     * Schedules a reminder, or updates an existing active reminder for the same
     * task within [DEDUP_WINDOW_MS] to prevent duplicates when rescheduling.
     */
    suspend operator fun invoke(reminder: Reminder): Long {
        require(reminder.taskId > 0) { "Task id required" }
        require(reminder.triggerAtMs > System.currentTimeMillis()) {
            "Reminder time must be in the future"
        }
        val existing = repository.getActiveRemindersForTask(reminder.taskId)
        val duplicate = existing.firstOrNull {
            abs(it.triggerAtMs - reminder.triggerAtMs) <= DEDUP_WINDOW_MS
        }
        if (duplicate != null) {
            repository.updateReminder(
                duplicate.copy(
                    triggerAtMs = reminder.triggerAtMs,
                    repeatType = reminder.repeatType,
                    repeatIntervalMs = reminder.repeatIntervalMs,
                    isActive = true,
                ),
            )
            return duplicate.id
        }
        return repository.scheduleReminder(reminder)
    }

    companion object {
        const val DEDUP_WINDOW_MS = 60_000L
    }
}
