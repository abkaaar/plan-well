package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.Priority
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.repository.ReminderRepository
import com.planwell.app.domain.usecase.task.CreateTaskUseCase
import javax.inject.Inject
import kotlin.math.abs

/**
 * Relationship lifecycle (explicit):
 * - Soft-delete / archive / restore opportunity: links and tasks are untouched.
 * - Permanent delete opportunity: Room CASCADE removes link rows only; tasks stay.
 * - Soft-delete task: link remains; opportunity UI may show the task as deleted.
 * - Permanent delete task: CASCADE removes that link only; opportunity stays.
 * - Never move/delete unrelated tasks when opportunity state changes.
 */

data class CreateTaskFromOpportunityResult(
    val taskId: Long,
    val alreadyLinked: Boolean = false,
)

class CreateTaskFromOpportunityUseCase @Inject constructor(
    private val opportunityRepository: OpportunityRepository,
    private val createTask: CreateTaskUseCase,
) {
    /**
     * Creates a task via the existing create pipeline and links it.
     * [dueDateMs] must be null or a user-confirmed / explicitly chosen time —
     * never an unconfirmed extracted date.
     */
    suspend operator fun invoke(
        opportunityId: Long,
        title: String? = null,
        description: String? = null,
        dueDateMs: Long? = null,
        useConfirmedDeadlineAsDue: Boolean = true,
    ): CreateTaskFromOpportunityResult {
        require(opportunityId > 0) { "Invalid opportunity" }
        val opportunity = opportunityRepository.getOpportunity(opportunityId)
            ?: throw IllegalArgumentException("Opportunity not found")

        val confirmedDue = if (useConfirmedDeadlineAsDue) {
            confirmedDeadlineMs(opportunity)
        } else {
            null
        }
        val due = dueDateMs ?: confirmedDue
        // Safety: refuse if caller somehow passes an unconfirmed extracted value
        if (due != null) {
            val matchesUnconfirmedOnly = opportunity.dates.any {
                !it.isUserConfirmed && it.extractedDateMs == due && it.dateMs == null
            } && opportunity.dates.none { it.isUserConfirmed && it.dateMs == due }
            require(!matchesUnconfirmedOnly) {
                "Cannot use an unconfirmed extracted date as a task due date"
            }
        }

        val taskTitle = (title ?: opportunity.title).trim()
        val taskDescription = description
            ?: buildString {
                opportunity.description?.let { append(it) }
                opportunity.sourceUrl?.let {
                    if (isNotEmpty()) append("\n\n")
                    append("Source: ").append(it)
                }
            }.ifBlank { null }

        val taskId = createTask(
            Task(
                title = taskTitle,
                description = taskDescription,
                dueDateMs = due,
                priority = Priority.NONE,
                categoryId = opportunity.categoryId,
            ),
        )
        opportunityRepository.linkTask(opportunityId, taskId)
        return CreateTaskFromOpportunityResult(taskId = taskId)
    }
}

class ScheduleConfirmedDeadlineReminderUseCase @Inject constructor(
    private val opportunityRepository: OpportunityRepository,
    private val reminderRepository: ReminderRepository,
) {
    /**
     * Schedules a reminder on [taskId] for a **confirmed** opportunity date.
     * Deduplicates active reminders within [DEDUP_WINDOW_MS] of the same trigger.
     */
    suspend operator fun invoke(
        opportunityId: Long,
        taskId: Long,
        opportunityDateId: Long,
        triggerAtMs: Long? = null,
        repeatType: RepeatType = RepeatType.NONE,
    ): Long {
        require(opportunityId > 0 && taskId > 0 && opportunityDateId > 0)
        val opportunity = opportunityRepository.getOpportunity(opportunityId)
            ?: throw IllegalArgumentException("Opportunity not found")
        require(taskId in opportunity.linkedTaskIds) {
            "Task is not linked to this opportunity"
        }
        val date = opportunity.dates.firstOrNull { it.id == opportunityDateId }
            ?: throw IllegalArgumentException("Date not found")
        require(date.isUserConfirmed && date.dateMs != null) {
            "Confirm the date before scheduling a reminder"
        }
        val trigger = triggerAtMs ?: date.dateMs
        require(trigger > System.currentTimeMillis()) {
            "Reminder time must be in the future"
        }

        val existing = reminderRepository.getActiveRemindersForTask(taskId)
        val duplicate = existing.firstOrNull { abs(it.triggerAtMs - trigger) <= DEDUP_WINDOW_MS }
        if (duplicate != null) {
            val updated = duplicate.copy(
                triggerAtMs = trigger,
                repeatType = repeatType,
                isActive = true,
            )
            reminderRepository.updateReminder(updated)
            return duplicate.id
        }
        return reminderRepository.scheduleReminder(
            Reminder(
                taskId = taskId,
                triggerAtMs = trigger,
                repeatType = repeatType,
            ),
        )
    }

    companion object {
        const val DEDUP_WINDOW_MS = 60_000L
    }
}

/** Optional preparation steps for user review — never auto-created as tasks. */
object OpportunityPrepSuggestions {
    fun forOpportunity(opportunity: Opportunity): List<String> {
        val suggestions = mutableListOf<String>()
        val hasDeadline = opportunity.dates.any {
            it.type == OpportunityDateType.APPLICATION_DEADLINE && it.isUserConfirmed
        }
        val hasEvent = opportunity.dates.any {
            it.type == OpportunityDateType.EVENT && it.isUserConfirmed
        }
        when (opportunity.type) {
            OpportunityType.DEADLINE, OpportunityType.OPPORTUNITY -> {
                if (hasDeadline || opportunity.type == OpportunityType.DEADLINE) {
                    suggestions += "Gather required documents"
                    suggestions += "Draft application / submission"
                    suggestions += "Submit before deadline"
                }
            }
            OpportunityType.EVENT, OpportunityType.ANNOUNCEMENT -> {
                if (hasEvent || opportunity.type == OpportunityType.EVENT) {
                    suggestions += "Add to calendar"
                    suggestions += "Prepare questions or materials"
                    suggestions += "Confirm attendance / RSVP"
                }
            }
            OpportunityType.OTHER -> Unit
        }
        if (suggestions.isEmpty()) {
            suggestions += "Review details and decide next step"
            suggestions += "Set a reminder if needed"
        }
        return suggestions.distinct()
    }
}

fun confirmedDeadlineMs(opportunity: Opportunity): Long? {
    val deadline = opportunity.dates.firstOrNull {
        it.type == OpportunityDateType.APPLICATION_DEADLINE && it.isUserConfirmed && it.dateMs != null
    }?.dateMs
    if (deadline != null) return deadline
    return opportunity.dates.firstOrNull {
        it.type == OpportunityDateType.EVENT && it.isUserConfirmed && it.dateMs != null
    }?.dateMs
}
