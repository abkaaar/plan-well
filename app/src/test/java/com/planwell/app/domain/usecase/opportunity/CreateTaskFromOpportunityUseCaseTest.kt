package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.usecase.task.CreateTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTaskFromOpportunityUseCaseTest {

    private val opportunityRepository: OpportunityRepository = mockk(relaxed = true)
    private val createTask: CreateTaskUseCase = mockk()
    private lateinit var useCase: CreateTaskFromOpportunityUseCase

    private val futureConfirmed = System.currentTimeMillis() + 86_400_000L

    @Before
    fun setUp() {
        useCase = CreateTaskFromOpportunityUseCase(opportunityRepository, createTask)
    }

    @Test
    fun createsTaskLinkedWithConfirmedDeadlineDue() = runTest {
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "Internship",
            description = "Apply soon",
            sourceUrl = "https://example.com",
            dates = listOf(
                OpportunityDate(
                    id = 9,
                    opportunityId = 1,
                    type = OpportunityDateType.APPLICATION_DEADLINE,
                    dateMs = futureConfirmed,
                    isUserConfirmed = true,
                ),
            ),
        )
        val taskSlot = slot<Task>()
        coEvery { createTask(capture(taskSlot)) } returns 42L

        val result = useCase(opportunityId = 1L)

        assertEquals(42L, result.taskId)
        assertEquals("Internship", taskSlot.captured.title)
        assertEquals(futureConfirmed, taskSlot.captured.dueDateMs)
        assertTrue(taskSlot.captured.description!!.contains("https://example.com"))
        coVerify { opportunityRepository.linkTask(1L, 42L) }
    }

    @Test
    fun rejectsUnconfirmedExtractedDateAsDue() = runTest {
        val extracted = System.currentTimeMillis() + 10_000L
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "Event",
            dates = listOf(
                OpportunityDate(
                    id = 2,
                    opportunityId = 1,
                    type = OpportunityDateType.EVENT,
                    extractedDateMs = extracted,
                    isUserConfirmed = false,
                ),
            ),
        )

        val error = runCatching {
            useCase(opportunityId = 1L, dueDateMs = extracted, useConfirmedDeadlineAsDue = false)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        coVerify(exactly = 0) { createTask(any()) }
    }

    @Test
    fun multipleTasksCanBeLinked() = runTest {
        coEvery { opportunityRepository.getOpportunity(1L) } returns Opportunity(
            id = 1,
            title = "Fair",
        )
        coEvery { createTask(any()) } returnsMany listOf(10L, 11L)

        assertEquals(10L, useCase(1L, title = "Register").taskId)
        assertEquals(11L, useCase(1L, title = "Prepare").taskId)
        coVerify { opportunityRepository.linkTask(1L, 10L) }
        coVerify { opportunityRepository.linkTask(1L, 11L) }
    }
}
