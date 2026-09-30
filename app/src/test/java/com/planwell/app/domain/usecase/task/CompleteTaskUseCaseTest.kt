package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CompleteTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private lateinit var useCase: CompleteTaskUseCase

    @Before
    fun setUp() {
        useCase = CompleteTaskUseCase(repository)
    }

    @Test
    fun completeCallsRepository() = runTest {
        useCase(7L, completed = true)
        coVerify { repository.completeTask(7L) }
    }

    @Test
    fun uncompleteCallsRepository() = runTest {
        useCase(7L, completed = false)
        coVerify { repository.uncompleteTask(7L) }
    }
}
