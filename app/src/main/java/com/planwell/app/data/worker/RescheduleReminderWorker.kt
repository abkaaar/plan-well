package com.planwell.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.planwell.app.data.alarm.ReminderScheduler
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class RescheduleReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId < 0) return Result.failure()

        val reminder = reminderRepository.getReminder(reminderId) ?: return Result.success()
        when (reminder.repeatType) {
            RepeatType.NONE -> {
                reminderRepository.cancelReminder(reminderId)
                reminderScheduler.cancel(reminderId)
            }
            RepeatType.DAILY -> {
                val next = reminder.copy(
                    triggerAtMs = reminder.triggerAtMs + TimeUnit.DAYS.toMillis(1),
                    isActive = true,
                )
                reminderRepository.updateReminder(next)
                reminderScheduler.schedule(next)
            }
            RepeatType.WEEKLY -> {
                val next = reminder.copy(
                    triggerAtMs = reminder.triggerAtMs + TimeUnit.DAYS.toMillis(7),
                    isActive = true,
                )
                reminderRepository.updateReminder(next)
                reminderScheduler.schedule(next)
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
