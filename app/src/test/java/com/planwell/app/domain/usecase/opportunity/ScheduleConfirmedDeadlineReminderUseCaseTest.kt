package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleConfirmedDeadlineReminderUseCaseTest {

    private val opportunityRepository: OpportunityRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private lateinit var useCase: ScheduleConfirmedDeadlineReminderUseCase

    private val trigger = System.currentTimeMillis() + 3_600_000L

    @Before
    fun setUp() {
        useCase = ScheduleConfirmedDeadlineReminderUseCase(
            opportunityRepository,
            reminderRepository,
        )
    }

    @Test
    fun refusesUnconfirmedDate() = runTest {
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "x",
            linkedTaskIds = listOf(5L),
            dates = listOf(
                OpportunityDate(
                    id = 2,
                    opportunityId = 1,
                    type = OpportunityDateType.APPLICATION_DEADLINE,
                    extractedDateMs = trigger,
                    isUserConfirmed = false,
                ),
            ),
        )

        val error = runCatching {
            useCase(opportunityId = 1L, taskId = 5L, opportunityDateId = 2L)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        coVerify(exactly = 0) { reminderRepository.scheduleReminder(any()) }
    }

    @Test
    fun dedupesExistingReminderWithinWindow() = runTest {
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "x",
            linkedTaskIds = listOf(5L),
            dates = listOf(
                OpportunityDate(
                    id = 2,
                    opportunityId = 1,
                    type = OpportunityDateType.EVENT,
                    dateMs = trigger,
                    isUserConfirmed = true,
                ),
            ),
        )
        coEvery { reminderRepository.getActiveRemindersForTask(5L) } returns listOf(
            Reminder(id = 99, taskId = 5, triggerAtMs = trigger + 30_000L),
        )

        val id = useCase(opportunityId = 1L, taskId = 5L, opportunityDateId = 2L)

        assertEquals(99L, id)
        coVerify { reminderRepository.updateReminder(any()) }
        coVerify(exactly = 0) { reminderRepository.scheduleReminder(any()) }
    }

    @Test
    fun schedulesWhenNoDuplicate() = runTest {
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "x",
            linkedTaskIds = listOf(5L),
            dates = listOf(
                OpportunityDate(
                    id = 2,
                    opportunityId = 1,
                    type = OpportunityDateType.APPLICATION_DEADLINE,
                    dateMs = trigger,
                    isUserConfirmed = true,
                ),
            ),
        )
        coEvery { reminderRepository.getActiveRemindersForTask(5L) } returns emptyList()
        coEvery { reminderRepository.scheduleReminder(any()) } returns 7L

        val id = useCase(opportunityId = 1L, taskId = 5L, opportunityDateId = 2L)
        assertEquals(7L, id)
        coVerify { reminderRepository.scheduleReminder(any()) }
    }
}
