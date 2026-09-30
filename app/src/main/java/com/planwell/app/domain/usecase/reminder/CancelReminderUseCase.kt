package com.planwell.app.domain.usecase.reminder

import com.planwell.app.domain.repository.ReminderRepository
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
) {
    suspend operator fun invoke(reminderId: Long) {
        repository.cancelReminder(reminderId)
    }
}
