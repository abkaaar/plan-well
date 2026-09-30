package com.planwell.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.entity.CategoryEntity
import com.planwell.app.data.local.db.entity.ReminderEntity
import com.planwell.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

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
    fun insertAndSoftDelete() = runTest {
        val id = db.taskDao().insertTask(TaskEntity(title = "Test"))
        val active = db.taskDao().getActiveTasks().first()
        assertEquals(1, active.size)

        db.taskDao().softDelete(id)
        assertTrue(db.taskDao().getActiveTasks().first().isEmpty())
        assertEquals(1, db.taskDao().getDeletedTasks().first().size)
    }

    @Test
    fun ftsSearchFindsTitle() = runTest {
        db.taskDao().insertTask(TaskEntity(title = "Buy groceries", description = "milk"))
        db.taskDao().insertTask(TaskEntity(title = "Pay rent"))

        val results = db.taskFtsDao().search("groceries*").first()
        assertEquals(1, results.size)
        assertEquals("Buy groceries", results.first().title)
    }

    @Test
    fun deleteCategoryNullifiesTaskCategoryId() = runTest {
        val catId = db.categoryDao().insert(CategoryEntity(name = "Work", colorHex = "#2563EB"))
        val taskId = db.taskDao().insertTask(TaskEntity(title = "Report", categoryId = catId))

        db.categoryDao().deleteById(catId)
        val task = db.taskDao().getTaskById(taskId)
        assertNull(task?.categoryId)
    }

    @Test
    fun permanentlyDeleteTaskCascadesReminders() = runTest {
        val taskId = db.taskDao().insertTask(TaskEntity(title = "With reminder"))
        db.reminderDao().insertReminder(
            ReminderEntity(
                taskId = taskId,
                triggerAtMs = System.currentTimeMillis() + 60_000,
            ),
        )

        val entity = db.taskDao().getTaskById(taskId)!!
        db.taskDao().permanentlyDelete(entity)

        assertTrue(db.reminderDao().getActiveRemindersForTask(taskId).isEmpty())
        assertNull(db.taskDao().getTaskById(taskId))
    }
}
