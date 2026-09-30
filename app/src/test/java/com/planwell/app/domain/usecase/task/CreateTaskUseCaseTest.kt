package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.model.Priority
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTaskUseCaseTest {

    private val repository: TaskRepository = mockk()
    private lateinit var useCase: CreateTaskUseCase

    @Before
    fun setUp() {
        useCase = CreateTaskUseCase(repository)
    }

    @Test
    fun blankTitleThrows() = runTest {
        val error = runCatching { useCase(Task(title = "  ")) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun titleTooLongThrows() = runTest {
        val error = runCatching { useCase(Task(title = "A".repeat(101))) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun validTaskDelegatesToRepository() = runTest {
        coEvery { repository.createTask(any()) } returns 42L

        val id = useCase(
            Task(
                title = "  Buy milk  ",
                description = "  2L  ",
                priority = Priority.LOW,
            ),
        )

        assertEquals(42L, id)
        coVerify {
            repository.createTask(
                match {
                    it.title == "Buy milk" &&
                        it.description == "2L" &&
                        it.priority == Priority.LOW
                },
            )
        }
    }
}
