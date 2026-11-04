package com.planwell.app.domain.agenda

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.Task

/**
 * Pure, rule-based agenda assembly from existing task / reminder / opportunity data.
 * Does not invent durations, move tasks, or create calendar commitments.
 */
object DailyAgendaBuilder {

    const val BUSY_DAY_THRESHOLD = 6
    const val UPCOMING_DEADLINE_LOOKAHEAD_MS = 2L * 24 * 60 * 60 * 1000

    fun build(
        day: DayBounds,
        tasks: List<Task>,
        reminders: List<Reminder>,
        opportunities: List<Opportunity>,
        nowMs: Long = System.currentTimeMillis(),
        taskTitleById: Map<Long, String> = tasks.associate { it.id to it.title },
    ): DailyAgenda {
        val items = mutableListOf<AgendaItem>()

        tasks.filter { !it.isDeleted }.forEach { task ->
            val due = task.dueDateMs ?: return@forEach
            when {
                day.contains(due) -> items += AgendaItem(
                    id = "task-due-${task.id}",
                    kind = AgendaItemKind.TASK_DUE,
                    section = AgendaSection.TODAY,
                    title = task.title,
                    subtitle = if (task.isCompleted) "Task due · completed" else "Task due",
                    sortTimeMs = due,
                    isCompleted = task.isCompleted,
                    taskId = task.id,
                )
                !task.isCompleted && day.isBefore(due) -> items += AgendaItem(
                    id = "task-overdue-${task.id}",
                    kind = AgendaItemKind.TASK_OVERDUE,
                    section = AgendaSection.OVERDUE,
                    title = task.title,
                    subtitle = "Overdue task",
                    sortTimeMs = due,
                    isCompleted = false,
                    taskId = task.id,
                )
            }
        }

        reminders.filter { it.isActive }.forEach { reminder ->
            if (!day.contains(reminder.triggerAtMs)) return@forEach
            val taskTitle = taskTitleById[reminder.taskId]
            items += AgendaItem(
                id = "reminder-${reminder.id}",
                kind = AgendaItemKind.REMINDER,
                section = AgendaSection.TODAY,
                title = taskTitle?.let { "Reminder: $it" } ?: "Reminder",
                subtitle = "Scheduled reminder",
                sortTimeMs = reminder.triggerAtMs,
                taskId = reminder.taskId,
                reminderId = reminder.id,
            )
        }

        opportunities.filter { !it.isDeleted }.forEach { opportunity ->
            opportunity.dates
                .filter { it.isUserConfirmed }
                .forEach { date ->
                    val ms = date.dateMs ?: return@forEach
                    if (!day.contains(ms)) return@forEach
                    val isDeadline = date.type == OpportunityDateType.APPLICATION_DEADLINE
                    items += AgendaItem(
                        id = "opp-date-${date.id}",
                        kind = if (isDeadline) {
                            AgendaItemKind.OPPORTUNITY_DEADLINE
                        } else {
                            AgendaItemKind.OPPORTUNITY_EVENT
                        },
                        section = AgendaSection.TODAY,
                        title = opportunity.title,
                        subtitle = when (date.type) {
                            OpportunityDateType.APPLICATION_DEADLINE -> "Opportunity deadline"
                            OpportunityDateType.EVENT -> "Scheduled event"
                            OpportunityDateType.OTHER -> "Confirmed date"
                        },
                        sortTimeMs = ms,
                        opportunityId = opportunity.id,
                    )
                }
        }

        val ordered = items.sortedWith(
            compareBy<AgendaItem> { it.section.ordinal }
                .thenBy { it.sortTimeMs }
                .thenBy { it.title },
        )

        return DailyAgenda(
            day = day,
            items = ordered,
            suggestions = buildSuggestions(day, ordered, opportunities, nowMs),
        )
    }

    private fun buildSuggestions(
        day: DayBounds,
        items: List<AgendaItem>,
        opportunities: List<Opportunity>,
        nowMs: Long,
    ): List<AgendaSuggestion> {
        val suggestions = mutableListOf<AgendaSuggestion>()
        val todayCount = items.count {
            it.section == AgendaSection.TODAY && !it.isCompleted
        }
        if (todayCount >= BUSY_DAY_THRESHOLD) {
            suggestions += AgendaSuggestion(
                id = "busy-day",
                message = "Busy day — $todayCount commitments. Review priorities if needed.",
            )
        }
        val overdueCount = items.count { it.kind == AgendaItemKind.TASK_OVERDUE }
        if (overdueCount > 0) {
            suggestions += AgendaSuggestion(
                id = "overdue",
                message = "You have $overdueCount overdue task${if (overdueCount == 1) "" else "s"}.",
            )
        }
        val upcoming = opportunities
            .filter { !it.isDeleted }
            .flatMap { opp ->
                opp.dates
                    .mapNotNull { date ->
                        val ms = date.dateMs ?: return@mapNotNull null
                        if (
                            date.isUserConfirmed &&
                            date.type == OpportunityDateType.APPLICATION_DEADLINE &&
                            ms in (nowMs + 1)..(nowMs + UPCOMING_DEADLINE_LOOKAHEAD_MS)
                        ) {
                            opp.title
                        } else {
                            null
                        }
                    }
            }
            .distinct()
            .take(2)
        upcoming.forEach { title ->
            suggestions += AgendaSuggestion(
                id = "review-deadline-$title",
                message = "Review upcoming deadline: $title",
            )
        }
        return suggestions
    }
}
