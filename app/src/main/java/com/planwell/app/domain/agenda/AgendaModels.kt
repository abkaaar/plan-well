package com.planwell.app.domain.agenda

enum class AgendaItemKind {
    /** Task due on the selected day. */
    TASK_DUE,
    /** Incomplete task due before the selected day. */
    TASK_OVERDUE,
    /** Reminder that fires on the selected day. */
    REMINDER,
    /** Confirmed opportunity application deadline on the day. */
    OPPORTUNITY_DEADLINE,
    /** Confirmed opportunity event / appointment-like date on the day. */
    OPPORTUNITY_EVENT,
}

enum class AgendaSection {
    OVERDUE,
    TODAY,
}

data class AgendaItem(
    val id: String,
    val kind: AgendaItemKind,
    val section: AgendaSection,
    val title: String,
    val subtitle: String?,
    val sortTimeMs: Long,
    val isCompleted: Boolean = false,
    val taskId: Long? = null,
    val opportunityId: Long? = null,
    val reminderId: Long? = null,
)

data class AgendaSuggestion(
    val id: String,
    val message: String,
)

data class DailyAgenda(
    val day: DayBounds,
    val items: List<AgendaItem>,
    val suggestions: List<AgendaSuggestion>,
) {
    val overdueItems: List<AgendaItem> get() = items.filter { it.section == AgendaSection.OVERDUE }
    val todayItems: List<AgendaItem> get() = items.filter { it.section == AgendaSection.TODAY }
}
