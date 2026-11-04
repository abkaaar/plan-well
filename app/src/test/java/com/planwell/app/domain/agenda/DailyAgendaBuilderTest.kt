package com.planwell.app.domain.agenda

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyAgendaBuilderTest {

    private val zone = TimeZone.getTimeZone("UTC")

    private fun dayAround(year: Int, monthZero: Int, day: Int): Pair<DayBounds, Long> {
        val noon = Calendar.getInstance(zone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZero)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return DayBounds.forInstant(noon, zone) to noon
    }

    @Test
    fun dayBoundsUseLocalMidnightInclusiveEnd() {
        val (bounds, _) = dayAround(2026, Calendar.MARCH, 15)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = bounds.startMs }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(bounds.contains(bounds.startMs))
        assertTrue(bounds.contains(bounds.endMsInclusive))
        assertTrue(bounds.isBefore(bounds.startMs - 1))
    }

    @Test
    fun ordersTodayByTimeAndSeparatesOverdue() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val overdueDue = day.startMs - 3_600_000L
        val dueMorning = day.startMs + 9 * 3_600_000L
        val dueEvening = day.startMs + 18 * 3_600_000L

        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = listOf(
                Task(id = 1, title = "Evening", dueDateMs = dueEvening),
                Task(id = 2, title = "Late", dueDateMs = overdueDue),
                Task(id = 3, title = "Morning", dueDateMs = dueMorning),
            ),
            reminders = emptyList(),
            opportunities = emptyList(),
        )

        assertEquals(1, agenda.overdueItems.size)
        assertEquals("Late", agenda.overdueItems.first().title)
        assertEquals(
            listOf("Morning", "Evening"),
            agenda.todayItems.map { it.title },
        )
    }

    @Test
    fun distinguishesEventFromDeadline() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val at = day.startMs + 14 * 3_600_000L
        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = emptyList(),
            reminders = emptyList(),
            opportunities = listOf(
                Opportunity(
                    id = 1,
                    title = "Career fair",
                    dates = listOf(
                        OpportunityDate(
                            id = 10,
                            type = OpportunityDateType.EVENT,
                            dateMs = at,
                            isUserConfirmed = true,
                        ),
                        OpportunityDate(
                            id = 11,
                            type = OpportunityDateType.APPLICATION_DEADLINE,
                            dateMs = at + 60_000,
                            isUserConfirmed = true,
                        ),
                    ),
                ),
            ),
        )
        assertTrue(agenda.todayItems.any { it.kind == AgendaItemKind.OPPORTUNITY_EVENT })
        assertTrue(agenda.todayItems.any { it.kind == AgendaItemKind.OPPORTUNITY_DEADLINE })
    }

    @Test
    fun ignoresUnconfirmedOpportunityDates() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val at = day.startMs + 10 * 3_600_000L
        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = emptyList(),
            reminders = emptyList(),
            opportunities = listOf(
                Opportunity(
                    id = 1,
                    title = "Draft",
                    dates = listOf(
                        OpportunityDate(
                            id = 1,
                            type = OpportunityDateType.APPLICATION_DEADLINE,
                            extractedDateMs = at,
                            isUserConfirmed = false,
                        ),
                    ),
                ),
            ),
        )
        assertTrue(agenda.todayItems.none { it.opportunityId == 1L })
    }

    @Test
    fun includesRemindersOnDay() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val trigger = day.startMs + 8 * 3_600_000L
        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = listOf(Task(id = 5, title = "Call coach")),
            reminders = listOf(
                Reminder(id = 9, taskId = 5, triggerAtMs = trigger, isActive = true),
            ),
            opportunities = emptyList(),
        )
        assertTrue(agenda.todayItems.any { it.kind == AgendaItemKind.REMINDER && it.taskId == 5L })
    }

    @Test
    fun emptyDayHasNoItems() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = listOf(Task(id = 1, title = "No due")),
            reminders = emptyList(),
            opportunities = emptyList(),
        )
        assertTrue(agenda.items.isEmpty())
    }

    @Test
    fun busyDaySuggestionWhenOverloaded() {
        val (day, noon) = dayAround(2026, Calendar.MARCH, 15)
        val tasks = (1..DailyAgendaBuilder.BUSY_DAY_THRESHOLD).map { i ->
            Task(
                id = i.toLong(),
                title = "T$i",
                dueDateMs = day.startMs + i * 3_600_000L,
            )
        }
        val agenda = DailyAgendaBuilder.build(
            day = day,
            nowMs = noon,
            tasks = tasks,
            reminders = emptyList(),
            opportunities = emptyList(),
        )
        assertTrue(agenda.suggestions.any { it.id == "busy-day" })
    }
}
