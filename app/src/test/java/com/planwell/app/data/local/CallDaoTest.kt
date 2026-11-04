package com.planwell.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.entity.CallEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallDaoTest {

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
    fun insertAndGetPendingByScheduledAt() = runTest {
        val now = System.currentTimeMillis()
        val dao = db.callDao()

        val pendingId = dao.insert(
            CallEntity(
                contactId = "42",
                displayName = "Ada",
                phoneNumber = "+15551234567",
                scheduledAtMs = now + 120_000,
                notes = "Check in",
                isCompleted = false,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        dao.insert(
            CallEntity(
                contactId = null,
                displayName = "Past",
                phoneNumber = "+15550001111",
                scheduledAtMs = now - 1_000,
                notes = null,
                isCompleted = false,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        dao.insert(
            CallEntity(
                contactId = null,
                displayName = "Done",
                phoneNumber = "+15550002222",
                scheduledAtMs = now + 60_000,
                notes = null,
                isCompleted = true,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )

        val pending = dao.getPending(now)
        assertEquals(1, pending.size)
        assertEquals(pendingId, pending.first().id)
        assertEquals("Ada", pending.first().displayName)
    }

    @Test
    fun markCompletedRemovesFromPending() = runTest {
        val now = System.currentTimeMillis()
        val dao = db.callDao()
        val id = dao.insert(
            CallEntity(
                contactId = null,
                displayName = "Bob",
                phoneNumber = "+15557654321",
                scheduledAtMs = now + 60_000,
                notes = null,
                isCompleted = false,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )

        dao.markCompleted(id, now + 1)
        assertTrue(dao.getPending(now).isEmpty())
        assertEquals(true, dao.getById(id)?.isCompleted)
    }
}
