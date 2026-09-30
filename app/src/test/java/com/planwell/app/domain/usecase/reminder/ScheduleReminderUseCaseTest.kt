package com.planwell.app.domain.usecase.reminder

import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleReminderUseCaseTest {

    private val repository: ReminderRepository = mockk()
    private lateinit var useCase: ScheduleReminderUseCase

    @Before
    fun setUp() {
        useCase = ScheduleReminderUseCase(repository)
    }

    @Test
    fun pastTriggerThrows() = runTest {
        val error = runCatching {
            useCase(
                Reminder(
                    taskId = 1L,
                    triggerAtMs = System.currentTimeMillis() - 1_000,
                    repeatType = RepeatType.NONE,
                ),
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun invalidTaskIdThrows() = runTest {
        val error = runCatching {
            useCase(
                Reminder(
                    taskId = 0L,
                    triggerAtMs = System.currentTimeMillis() + 60_000,
                    repeatType = RepeatType.NONE,
                ),
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun futureReminderSchedules() = runTest {
        coEvery { repository.scheduleReminder(any()) } returns 9L
        val trigger = System.currentTimeMillis() + 120_000

        val id = useCase(
            Reminder(taskId = 3L, triggerAtMs = trigger, repeatType = RepeatType.DAILY),
        )

        assertEquals(9L, id)
        coVerify { repository.scheduleReminder(match { it.taskId == 3L && it.triggerAtMs == trigger }) }
    }
}
