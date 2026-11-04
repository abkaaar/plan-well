package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.repository.OpportunityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateOpportunityUseCaseTest {

    private val repository: OpportunityRepository = mockk()
    private lateinit var create: CreateOpportunityUseCase
    private lateinit var confirmDate: ConfirmOpportunityDateUseCase
    private lateinit var linkTask: LinkOpportunityTaskUseCase

    @Before
    fun setUp() {
        create = CreateOpportunityUseCase(repository)
        confirmDate = ConfirmOpportunityDateUseCase(repository)
        linkTask = LinkOpportunityTaskUseCase(repository)
    }

    @Test
    fun blankTitleThrows() = runTest {
        val error = runCatching { create(Opportunity(title = "  ")) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun titleTooLongThrows() = runTest {
        val error = runCatching {
            create(Opportunity(title = "A".repeat(201)))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun validOpportunityDelegates() = runTest {
        coEvery { repository.createOpportunity(any()) } returns 7L

        val id = create(
            Opportunity(
                title = "  Career fair  ",
                description = "  Campus hall  ",
                type = OpportunityType.EVENT,
                sourceUrl = " https://example.com ",
                status = OpportunityStatus.NEEDS_REVIEW,
                dates = listOf(
                    OpportunityDate(
                        type = OpportunityDateType.EVENT,
                        extractedDateMs = 123L,
                    ),
                ),
            ),
        )

        assertEquals(7L, id)
        coVerify {
            repository.createOpportunity(
                match {
                    it.title == "Career fair" &&
                        it.description == "Campus hall" &&
                        it.sourceUrl == "https://example.com" &&
                        it.dates.size == 1
                },
            )
        }
    }

    @Test
    fun confirmDateRequiresPositiveId() = runTest {
        val error = runCatching { confirmDate(0L, 1L) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun linkTaskDelegates() = runTest {
        coEvery { repository.linkTask(1L, 2L) } returns Unit
        linkTask(1L, 2L)
        coVerify { repository.linkTask(1L, 2L) }
    }
}
