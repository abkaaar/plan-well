package com.planwell.app.domain.usecase.task

import app.cash.turbine.test
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchTasksUseCaseTest {

    private val repository: TaskRepository = mockk()
    private lateinit var useCase: SearchTasksUseCase

    @Before
    fun setUp() {
        useCase = SearchTasksUseCase(repository)
    }

    @Test
    fun emptyQueryReturnsEmptyWithoutHittingRepository() = runTest {
        useCase("").test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
        verify(exactly = 0) { repository.searchTasks(any()) }
    }

    @Test
    fun validQuerySanitizesAndDelegates() = runTest {
        val expected = listOf(Task(id = 1L, title = "Buy groceries"))
        every { repository.searchTasks("grocery*") } returns flowOf(expected)

        useCase("grocery").test {
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun sanitizeStripsOperatorsAndAddsPrefix() {
        assertEquals("buy* milk*", SearchTasksUseCase.sanitizeFtsQuery("buy milk"))
        assertEquals("", SearchTasksUseCase.sanitizeFtsQuery("   "))
        assertEquals("hello*", SearchTasksUseCase.sanitizeFtsQuery("\"hello*\""))
    }
}
