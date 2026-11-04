package com.planwell.app.domain.importer

import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.reminder.ScheduleReminderUseCase
import com.planwell.app.domain.usecase.task.CreateTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveImportedEntryUseCaseTest {

    private val createTask = mockk<CreateTaskUseCase>()
    private val scheduleReminder = mockk<ScheduleReminderUseCase>()
    private val useCase = SaveImportedEntryUseCase(createTask, scheduleReminder)

    private fun entry(
        kind: EntryKind = EntryKind.TASK,
        due: Long? = null,
        title: String = "Pay rent",
    ) = ImportedEntry(
        kind = kind,
        title = title,
        notes = "pay rent by the 5th",
        dueOrStartMs = due,
        timeWasExplicit = due != null,
        phone = null,
        address = null,
        url = null,
        money = null,
        sourceImagePath = null,
        confidence = 0.8f,
    )

    @Test
    fun savesTaskAndSchedulesFutureReminder() = runTest {
        val due = System.currentTimeMillis() + 86_400_000L
        val taskSlot = slot<Task>()
        coEvery { createTask(capture(taskSlot)) } returns 42L
        coEvery { scheduleReminder(any()) } returns 1L

        val id = useCase(entry(due = due), nowMs = System.currentTimeMillis())

        assertEquals(42L, id)
        assertEquals("Pay rent", taskSlot.captured.title)
        assertEquals(due, taskSlot.captured.dueDateMs)
        coVerify { scheduleReminder(match<Reminder> { it.taskId == 42L && it.triggerAtMs == due }) }
    }

    @Test
    fun callRequiresDueDate() = runTest {
        try {
            useCase(entry(kind = EntryKind.CALL, due = null))
            assertTrue("expected failure", false)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Calls need"))
        }
        coVerify(exactly = 0) { createTask(any()) }
    }

    @Test
    fun eventPrefixesTitle() = runTest {
        val due = System.currentTimeMillis() + 3_600_000L
        val taskSlot = slot<Task>()
        coEvery { createTask(capture(taskSlot)) } returns 7L
        coEvery { scheduleReminder(any()) } returns 1L

        useCase(entry(kind = EntryKind.EVENT, title = "Town hall", due = due))

        assertTrue(taskSlot.captured.title.startsWith("Event "))
    }

    @Test
    fun pastDueStillCreatesTaskWithoutReminder() = runTest {
        val past = System.currentTimeMillis() - 60_000L
        coEvery { createTask(any()) } returns 9L

        useCase(entry(due = past), nowMs = System.currentTimeMillis())

        coVerify(exactly = 1) { createTask(any()) }
        coVerify(exactly = 0) { scheduleReminder(any()) }
    }
}
