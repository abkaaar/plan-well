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
        coEvery { repository.getActiveRemindersForTask(3L) } returns emptyList()
        coEvery { repository.scheduleReminder(any()) } returns 9L
        val trigger = System.currentTimeMillis() + 120_000

        val id = useCase(
            Reminder(taskId = 3L, triggerAtMs = trigger, repeatType = RepeatType.DAILY),
        )

        assertEquals(9L, id)
        coVerify { repository.scheduleReminder(match { it.taskId == 3L && it.triggerAtMs == trigger }) }
    }

    @Test
    fun nearbyExistingReminderIsUpdatedNotDuplicated() = runTest {
        val trigger = System.currentTimeMillis() + 120_000
        coEvery { repository.getActiveRemindersForTask(3L) } returns listOf(
            Reminder(id = 4L, taskId = 3L, triggerAtMs = trigger + 10_000L),
        )
        coEvery { repository.updateReminder(any()) } returns Unit

        val id = useCase(
            Reminder(taskId = 3L, triggerAtMs = trigger, repeatType = RepeatType.NONE),
        )

        assertEquals(4L, id)
        coVerify { repository.updateReminder(match { it.id == 4L && it.triggerAtMs == trigger }) }
        coVerify(exactly = 0) { repository.scheduleReminder(any()) }
    }
}
