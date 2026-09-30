package com.planwell.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.planwell.app.data.alarm.ReminderScheduler
import com.planwell.app.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RestoreRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        reminderRepository.getAllActiveReminders()
            .filter { it.triggerAtMs > now }
            .forEach { reminderScheduler.schedule(it) }
        return Result.success()
    }
}
