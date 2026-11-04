package com.planwell.app.data.attachment

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class FileOpportunityAttachmentStoreTest {

    private lateinit var store: FileOpportunityAttachmentStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = FileOpportunityAttachmentStore(context)
        store.rootDir().deleteRecursively()
    }

    @Test
    fun saveAndResolveRoundTrip() {
        val relative = store.save(
            opportunityId = 9L,
            displayName = "flyer.png",
            input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
        )
        val file = store.resolve(relative)
        assertTrue(file.exists())
        assertTrue(relative.startsWith("9/"))
        assertTrue(file.length() == 3L)
    }

    @Test
    fun deleteMissingPathIsSafe() {
        store.delete("missing/does-not-exist.bin")
        assertFalse(store.resolve("missing/does-not-exist.bin").exists())
    }

    @Test
    fun deleteAllForOpportunityRemovesDir() {
        val relative = store.save(
            opportunityId = 3L,
            displayName = "a.txt",
            input = ByteArrayInputStream("hi".toByteArray()),
        )
        assertTrue(store.resolve(relative).exists())
        store.deleteAllForOpportunity(3L)
        assertFalse(store.resolve(relative).exists())
    }
}
