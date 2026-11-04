package com.planwell.app.ui.opportunity

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.opportunity.ArchiveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.CaptureSharedContentUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveArchivedOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveDeletedOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveInboxOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.PermanentlyDeleteOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.RestoreOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.SoftDeleteOpportunityUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpportunityInboxViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeInbox: ObserveInboxOpportunitiesUseCase = mockk()
    private val observeArchived: ObserveArchivedOpportunitiesUseCase = mockk()
    private val observeDeleted: ObserveDeletedOpportunitiesUseCase = mockk()
    private val getCategories: GetCategoriesUseCase = mockk()
    private val captureShared: CaptureSharedContentUseCase = mockk(relaxed = true)
    private val softDelete: SoftDeleteOpportunityUseCase = mockk(relaxed = true)
    private val restore: RestoreOpportunityUseCase = mockk(relaxed = true)
    private val archive: ArchiveOpportunityUseCase = mockk(relaxed = true)
    private val permanentlyDelete: PermanentlyDeleteOpportunityUseCase = mockk(relaxed = true)

    private val inboxItems = listOf(
        Opportunity(id = 1, title = "Career fair", status = OpportunityStatus.NEEDS_REVIEW),
        Opportunity(id = 2, title = "Scholarship", status = OpportunityStatus.ACTIVE, categoryId = 9),
        Opportunity(id = 3, title = "Workshop", status = OpportunityStatus.COMPLETED),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeInbox() } returns flowOf(inboxItems)
        every { observeArchived() } returns flowOf(
            listOf(Opportunity(id = 4, title = "Old", status = OpportunityStatus.ARCHIVED)),
        )
        every { observeDeleted() } returns flowOf(emptyList())
        every { getCategories() } returns flowOf(listOf(Category(id = 9, name = "Work")))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = OpportunityInboxViewModel(
        observeInbox,
        observeArchived,
        observeDeleted,
        getCategories,
        captureShared,
        softDelete,
        restore,
        archive,
        permanentlyDelete,
    )

    private suspend fun ReceiveTurbine<OpportunityInboxUiState>.awaitLoaded(): OpportunityInboxUiState {
        var state = awaitItem()
        while (state.isLoading) {
            state = awaitItem()
        }
        return state
    }

    @Test
    fun inboxShowsNonArchivedItems() = runTest(dispatcher) {
        val vm = createVm()
        vm.uiState.test {
            val state = awaitLoaded()
            assertEquals(3, state.items.size)
            assertEquals(OpportunityListFilter.INBOX, state.statusFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun statusFilterNeedsReview() = runTest(dispatcher) {
        val vm = createVm()
        vm.uiState.test {
            awaitLoaded()
            vm.onStatusFilterChange(OpportunityListFilter.NEEDS_REVIEW)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.items.size)
            assertEquals("Career fair", state.items.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFiltersByTitle() = runTest(dispatcher) {
        val vm = createVm()
        vm.uiState.test {
            awaitLoaded()
            vm.onQueryChange("scholar")
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.items.size)
            assertEquals(2L, state.items.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryFilter() = runTest(dispatcher) {
        val vm = createVm()
        vm.uiState.test {
            awaitLoaded()
            vm.onCategoryFilterChange(9L)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.items.size)
            assertEquals("Scholarship", state.items.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteEmitsUndoEvent() = runTest(dispatcher) {
        coEvery { softDelete(any()) } returns Unit
        val vm = createVm()
        vm.events.test {
            vm.delete(inboxItems.first())
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is OpportunityInboxEvent.ShowUndoDelete)
            assertEquals(1L, (event as OpportunityInboxEvent.ShowUndoDelete).id)
            coVerify { softDelete(1L) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
