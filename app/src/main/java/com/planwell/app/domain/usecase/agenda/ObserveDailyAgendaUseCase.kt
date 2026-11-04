package com.planwell.app.domain.usecase.agenda

import com.planwell.app.domain.agenda.DailyAgenda
import com.planwell.app.domain.agenda.DailyAgendaBuilder
import com.planwell.app.domain.agenda.DayBounds
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.repository.ReminderRepository
import com.planwell.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveDailyAgendaUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val opportunityRepository: OpportunityRepository,
) {
    operator fun invoke(nowMs: Long = System.currentTimeMillis()): Flow<DailyAgenda> {
        val day = DayBounds.forInstant(nowMs)
        return combine(
            taskRepository.observeActiveTasks(),
            reminderRepository.observeAllActiveReminders(),
            opportunityRepository.observeInbox(),
        ) { tasks, reminders, opportunities ->
            DailyAgendaBuilder.build(
                day = day,
                tasks = tasks,
                reminders = reminders,
                opportunities = opportunities,
                nowMs = nowMs,
            )
        }
    }
}
