package com.planwell.app.ui.search

import app.cash.turbine.test
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.SearchTasksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val searchTasks: SearchTasksUseCase = mockk()
    private val completeTask: CompleteTaskUseCase = mockk(relaxed = true)
    private val getCategories: GetCategoriesUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { getCategories() } returns flowOf(listOf(Category(id = 1, name = "Work")))
        every { searchTasks("") } returns flowOf(emptyList())
        every { searchTasks("milk") } returns flowOf(listOf(Task(id = 5, title = "Buy milk")))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun debounceEmitsResultsAfter300ms() = runTest(dispatcher) {
        val vm = SearchViewModel(searchTasks, completeTask, getCategories)

        vm.uiState.test {
            skipItems(1) // initial
            vm.onQueryChange("milk")
            advanceTimeBy(299)
            // still previous/empty until debounce
            advanceTimeBy(2)
            advanceUntilIdle()
            val withResults = expectMostRecentItem()
            assertEquals("milk", withResults.query)
            assertEquals(1, withResults.results.size)
            assertEquals("Buy milk", withResults.results.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankQueryClearsResults() = runTest(dispatcher) {
        val vm = SearchViewModel(searchTasks, completeTask, getCategories)
        vm.onQueryChange("milk")
        advanceTimeBy(300)
        advanceUntilIdle()
        vm.onQueryChange("")
        advanceTimeBy(300)
        advanceUntilIdle()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
