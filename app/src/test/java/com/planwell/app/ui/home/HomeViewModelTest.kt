package com.planwell.app.ui.home

import app.cash.turbine.test
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.CompletionStatsUseCase
import com.planwell.app.domain.usecase.task.DeleteTaskUseCase
import com.planwell.app.domain.usecase.task.ExportTasksUseCase
import com.planwell.app.domain.usecase.task.GetTasksUseCase
import com.planwell.app.domain.usecase.task.RestoreTaskUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getTasks: GetTasksUseCase = mockk()
    private val getCategories: GetCategoriesUseCase = mockk()
    private val completionStats: CompletionStatsUseCase = mockk()
    private val completeTask: CompleteTaskUseCase = mockk(relaxed = true)
    private val deleteTask: DeleteTaskUseCase = mockk(relaxed = true)
    private val restoreTask: RestoreTaskUseCase = mockk(relaxed = true)
    private val exportTasks: ExportTasksUseCase = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { getTasks() } returns flowOf(
            listOf(
                Task(id = 1, title = "Work task", categoryId = 10),
                Task(id = 2, title = "Personal", categoryId = 20),
            ),
        )
        every { getCategories() } returns flowOf(
            listOf(Category(id = 10, name = "Work"), Category(id = 20, name = "Personal")),
        )
        every { completionStats(any()) } returns flowOf(CompletionStats(2, 1))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = HomeViewModel(
        getTasks,
        getCategories,
        completionStats,
        completeTask,
        deleteTask,
        restoreTask,
        exportTasks,
    )

    @Test
    fun filtersByCategory() = runTest(dispatcher) {
        val vm = createVm()

        vm.uiState.test {
            val initial = awaitItem()
            val ready = if (initial.isLoading) awaitItem() else initial
            assertEquals(2, ready.filteredTasks.size)

            vm.selectCategory(10)
            advanceUntilIdle()
            val filtered = awaitItem()
            assertEquals(listOf(1L), filtered.filteredTasks.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleCompleteDelegates() = runTest(dispatcher) {
        val vm = createVm()
        advanceUntilIdle()
        vm.toggleComplete(Task(id = 1, title = "Work task", isCompleted = false))
        advanceUntilIdle()
        coVerify { completeTask(1L, completed = true) }
    }
}
