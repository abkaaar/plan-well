package com.planwell.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.entity.OpportunityDateEntity
import com.planwell.app.data.local.db.entity.OpportunityEntity
import com.planwell.app.data.local.db.entity.OpportunityTaskLinkEntity
import com.planwell.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpportunityDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createRetrieveUpdateAndSoftDelete() = runTest {
        val id = db.opportunityDao().insert(
            OpportunityEntity(
                title = "Internship",
                description = "Apply by Friday",
                type = "DEADLINE",
                status = "NEEDS_REVIEW",
            ),
        )
        val loaded = db.opportunityDao().getById(id)!!
        assertEquals("Internship", loaded.title)

        db.opportunityDao().update(loaded.copy(title = "Summer Internship", status = "ACTIVE"))
        assertEquals("Summer Internship", db.opportunityDao().getById(id)!!.title)
        assertEquals(1, db.opportunityDao().observeInbox().first().size)

        db.opportunityDao().softDelete(id)
        assertTrue(db.opportunityDao().observeInbox().first().isEmpty())
        assertEquals(1, db.opportunityDao().observeDeleted().first().size)

        db.opportunityDao().restore(id)
        assertEquals(1, db.opportunityDao().observeInbox().first().size)
        assertFalse(db.opportunityDao().getById(id)!!.isDeleted)
    }

    @Test
    fun archiveAndUnarchive() = runTest {
        val id = db.opportunityDao().insert(OpportunityEntity(title = "Talk"))
        db.opportunityDao().updateStatus(id, "ARCHIVED")
        assertTrue(db.opportunityDao().observeInbox().first().isEmpty())
        assertEquals(1, db.opportunityDao().observeArchived().first().size)

        db.opportunityDao().updateStatus(id, "ACTIVE")
        assertEquals(1, db.opportunityDao().observeInbox().first().size)
    }

    @Test
    fun typedDatesAndConfirmation() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Hackathon"))
        val dateId = db.opportunityDateDao().insert(
            OpportunityDateEntity(
                opportunityId = oppId,
                type = "EVENT",
                extractedDateMs = 1_700_000_000_000L,
                isUserConfirmed = false,
            ),
        )
        val deadlineId = db.opportunityDateDao().insert(
            OpportunityDateEntity(
                opportunityId = oppId,
                type = "APPLICATION_DEADLINE",
                extractedDateMs = 1_700_100_000_000L,
                isUserConfirmed = false,
            ),
        )

        val dates = db.opportunityDateDao().getForOpportunity(oppId)
        assertEquals(2, dates.size)

        val event = db.opportunityDateDao().getById(dateId)!!
        db.opportunityDateDao().update(
            event.copy(dateMs = 1_700_000_000_000L, isUserConfirmed = true),
        )
        assertTrue(db.opportunityDateDao().getById(dateId)!!.isUserConfirmed)
        assertEquals(deadlineId, db.opportunityDateDao().getById(deadlineId)!!.id)
        assertFalse(db.opportunityDateDao().getById(deadlineId)!!.isUserConfirmed)
    }

    @Test
    fun linkMultipleTasksWithoutCouplingLifecycle() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Event"))
        val taskA = db.taskDao().insertTask(TaskEntity(title = "Register"))
        val taskB = db.taskDao().insertTask(TaskEntity(title = "Prepare talk"))

        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskA))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskB))
        assertEquals(listOf(taskA, taskB), db.opportunityTaskLinkDao().getTaskIds(oppId))

        db.opportunityDao().permanentlyDelete(oppId)
        assertNull(db.opportunityDao().getById(oppId))
        assertTrue(db.opportunityTaskLinkDao().getTaskIds(oppId).isEmpty())
        // Tasks remain
        assertEquals("Register", db.taskDao().getTaskById(taskA)!!.title)
        assertEquals("Prepare talk", db.taskDao().getTaskById(taskB)!!.title)
    }

    @Test
    fun deletingTaskRemovesLinkOnly() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Event"))
        val taskId = db.taskDao().insertTask(TaskEntity(title = "Todo"))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskId))

        val task = db.taskDao().getTaskById(taskId)!!
        db.taskDao().permanentlyDelete(task)

        assertTrue(db.opportunityTaskLinkDao().getTaskIds(oppId).isEmpty())
        assertEquals("Event", db.opportunityDao().getById(oppId)!!.title)
    }
}
