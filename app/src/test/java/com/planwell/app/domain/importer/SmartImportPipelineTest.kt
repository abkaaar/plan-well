package com.planwell.app.domain.importer

import android.content.Context
import com.planwell.app.data.ml.TextExtractor
import com.planwell.app.data.net.NetworkConnectivity
import com.planwell.app.data.net.OgMetadata
import com.planwell.app.data.net.OgMetadataExtractor
import com.planwell.app.data.preferences.UserPreferencesStore
import com.planwell.app.data.storage.ImageStore
import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.domain.classify.IntentClassifier
import com.planwell.app.domain.extract.EntityExtractor
import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.extract.ExtractedEntity
import com.planwell.app.domain.share.SharePayload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SmartImportPipelineTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val textExtractor = mockk<TextExtractor>(relaxed = true)
    private val entityExtractor = mockk<EntityExtractor>()
    private val entryBuilder = EntryBuilder(TitleExtractor(), IntentClassifier())
    private val imageStore = mockk<ImageStore>(relaxed = true)
    private val og = mockk<OgMetadataExtractor>()
    private val prefs = mockk<UserPreferencesStore>()
    private val connectivity = mockk<NetworkConnectivity>()

    private fun pipeline() = SmartImportPipeline(
        context = context,
        textExtractor = textExtractor,
        entityExtractor = entityExtractor,
        entryBuilder = entryBuilder,
        imageStore = imageStore,
        ogMetadataExtractor = og,
        preferences = prefs,
        connectivity = connectivity,
    )

    @Test
    fun textPayload_buildsTaskEntry() = runTest {
        coEvery { prefs.fetchLinkPreviewsOnce() } returns false
        every { connectivity.isOnline() } returns false
        coEvery { entityExtractor.extract(any(), any()) } returns emptyList()

        val stages = pipeline().process(
            SharePayload.Text(text = "pay rent by the 5th", subject = null),
        ).toList()

        val ready = stages.filterIsInstance<ImportStage.Ready>().single()
        assertEquals(EntryKind.TASK, ready.entry.kind)
        assertTrue(ready.entry.notes.contains("pay rent", ignoreCase = true))
        coVerify(exactly = 0) { og.fetch(any()) }
    }

    @Test
    fun skipsOgWhenOffline() = runTest {
        coEvery { prefs.fetchLinkPreviewsOnce() } returns true
        every { connectivity.isOnline() } returns false
        coEvery { entityExtractor.extract(any(), any()) } returns listOf(
            ExtractedEntity(
                type = EntityType.URL,
                rawText = "https://example.com",
                start = 0,
                end = 19,
                normalised = "https://example.com",
            ),
        )

        pipeline().process(
            SharePayload.Text("See https://example.com", null),
        ).toList()

        coVerify(exactly = 0) { og.fetch(any()) }
    }

    @Test
    fun fetchesOgWhenOnlineAndPrefOn() = runTest {
        coEvery { prefs.fetchLinkPreviewsOnce() } returns true
        every { connectivity.isOnline() } returns true
        coEvery { og.fetch(any()) } returns OgMetadata(
            title = "OG Title",
            description = "OG Desc",
            imageUrl = null,
        )
        coEvery { entityExtractor.extract(any(), any()) } returns emptyList()

        val ready = pipeline().process(
            SharePayload.Text("https://example.com/post", null),
        ).toList().filterIsInstance<ImportStage.Ready>().single()

        assertTrue(ready.entry.notes.contains("OG Title"))
        coVerify(atLeast = 1) { og.fetch(match { it.contains("example.com") }) }
    }

    @Test
    fun processOnce_throwsOnEmptyText() = runTest {
        coEvery { prefs.fetchLinkPreviewsOnce() } returns false
        every { connectivity.isOnline() } returns false

        try {
            pipeline().processOnce(SharePayload.Text("   ", null))
            assertTrue(false)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("text", ignoreCase = true))
        }
    }
}
