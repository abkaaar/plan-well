package com.planwell.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.entity.OpportunityEntity
import com.planwell.app.data.local.db.entity.OpportunityTaskLinkEntity
import com.planwell.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpportunityTaskLinkDaoTest {

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
    fun permanentDeleteOpportunityRemovesLinksOnly() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Opp"))
        val taskA = db.taskDao().insertTask(TaskEntity(title = "A"))
        val taskB = db.taskDao().insertTask(TaskEntity(title = "B"))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskA))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskB))

        db.opportunityDao().permanentlyDelete(oppId)

        assertTrue(db.opportunityTaskLinkDao().getTaskIds(oppId).isEmpty())
        assertEquals("A", db.taskDao().getTaskById(taskA)!!.title)
        assertEquals("B", db.taskDao().getTaskById(taskB)!!.title)
    }

    @Test
    fun permanentDeleteTaskRemovesLinkOnly() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Opp"))
        val taskId = db.taskDao().insertTask(TaskEntity(title = "Todo"))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskId))

        db.taskDao().permanentlyDelete(db.taskDao().getTaskById(taskId)!!)

        assertTrue(db.opportunityTaskLinkDao().getTaskIds(oppId).isEmpty())
        assertEquals("Opp", db.opportunityDao().getById(oppId)!!.title)
    }

    @Test
    fun softDeleteOpportunityKeepsLinksAndTasks() = runTest {
        val oppId = db.opportunityDao().insert(OpportunityEntity(title = "Opp"))
        val taskId = db.taskDao().insertTask(TaskEntity(title = "Todo"))
        db.opportunityTaskLinkDao().insert(OpportunityTaskLinkEntity(oppId, taskId))

        db.opportunityDao().softDelete(oppId)

        assertEquals(listOf(taskId), db.opportunityTaskLinkDao().getTaskIds(oppId))
        assertEquals("Todo", db.taskDao().getTaskById(taskId)!!.title)
        assertTrue(db.opportunityDao().getById(oppId)!!.isDeleted)
    }
}
