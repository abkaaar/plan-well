package com.planwell.app.domain.importer

import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.domain.model.Priority
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.reminder.ScheduleReminderUseCase
import com.planwell.app.domain.usecase.task.CreateTaskUseCase
import javax.inject.Inject

class SaveImportedEntryUseCase @Inject constructor(
    private val createTask: CreateTaskUseCase,
    private val scheduleReminder: ScheduleReminderUseCase,
) {
    suspend operator fun invoke(entry: ImportedEntry, nowMs: Long = System.currentTimeMillis()): Long {
        val kind = entry.kind
        if ((kind == EntryKind.CALL || kind == EntryKind.EVENT) && entry.dueOrStartMs == null) {
            error(
                if (kind == EntryKind.CALL) {
                    "Calls need a date and time"
                } else {
                    "Events need a date and time"
                },
            )
        }

        val title = buildTitle(entry).take(100)
        val description = buildDescription(entry).take(500).ifBlank { null }
        val taskId = createTask(
            Task(
                title = title,
                description = description,
                dueDateMs = entry.dueOrStartMs,
                priority = Priority.NONE,
                sourceType = SOURCE_TYPE_SHARE_IMPORT,
                sourceImagePath = entry.sourceImagePath?.takeIf { it.isNotBlank() },
                sourceUrl = entry.url?.takeIf { it.isNotBlank() },
            ),
        )

        val due = entry.dueOrStartMs
        if (due != null && due > nowMs) {
            runCatching {
                scheduleReminder(
                    Reminder(
                        taskId = taskId,
                        triggerAtMs = due,
                    ),
                )
            }
        }
        return taskId
    }

    private fun buildTitle(entry: ImportedEntry): String {
        val base = entry.title.trim().ifBlank { "Untitled" }
        return when (entry.kind) {
            EntryKind.TASK -> base
            EntryKind.CALL -> when {
                base.startsWith("Call ", ignoreCase = true) -> base
                !entry.phone.isNullOrBlank() &&
                    (base.equals("Untitled", true) || base.length < 3) -> "Call ${entry.phone}"
                else -> "Call $base"
            }
            EntryKind.EVENT -> when {
                base.startsWith("Event ", ignoreCase = true) -> base
                else -> "Event $base"
            }
        }
    }

    private fun buildDescription(entry: ImportedEntry): String {
        return buildString {
            append(entry.notes.trim())
            entry.phone?.takeIf { it.isNotBlank() && entry.kind != EntryKind.CALL }
                ?.let { append("\nPhone: $it") }
            entry.address?.takeIf { it.isNotBlank() }?.let { append("\nAddress: $it") }
            entry.money?.takeIf { it.isNotBlank() }?.let { append("\nAmount: $it") }
        }.trim()
    }

    companion object {
        const val SOURCE_TYPE_SHARE_IMPORT = "SHARE_IMPORT"
    }
}
