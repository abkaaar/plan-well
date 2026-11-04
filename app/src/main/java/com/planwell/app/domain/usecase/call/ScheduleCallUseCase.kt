package com.planwell.app.domain.usecase.call

import com.planwell.app.domain.model.ScheduledCall
import com.planwell.app.domain.repository.CallRepository
import javax.inject.Inject

class ScheduleCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    suspend operator fun invoke(call: ScheduledCall): Long {
        require(call.phoneNumber.isNotBlank()) { "Phone number required" }
        require(call.displayName.isNotBlank()) { "Display name required" }
        require(call.scheduledAtMs > System.currentTimeMillis()) {
            "Call time must be in the future"
        }
        return callRepository.schedule(call)
    }
}
