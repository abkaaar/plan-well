package com.planwell.app.data.ml

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextExtractorTest {

    private val bitmap: Bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)

    @Test
    fun mapBlocks_preservesOrderAndAveragesConfidence() {
        val result = TextExtractor.mapBlocks(
            fullText = "Headline Event\n15 March 2026\nCity Hall",
            blocks = listOf(
                "Headline Event" to listOf(0.8f),
                "15 March 2026" to listOf(0.8f, 1.0f),
                "City Hall" to emptyList(),
            ),
        )

        assertEquals("Headline Event\n15 March 2026\nCity Hall", result.fullText)
        assertEquals(3, result.blocks.size)
        assertEquals("Headline Event", result.blocks[0].text)
        assertEquals(0.8f, result.blocks[0].confidence, 0.001f)
        assertEquals("15 March 2026", result.blocks[1].text)
        assertEquals(0.9f, result.blocks[1].confidence, 0.001f)
        assertEquals("City Hall", result.blocks[2].text)
        assertEquals(0f, result.blocks[2].confidence, 0.001f)
    }

    @Test
    fun extract_onRecognizerFailure_returnsEmpty() = runTest {
        val extractor = TextExtractor.createForTest {
            Tasks.forException(IllegalStateException("boom"))
        }

        val result = extractor.extract(bitmap)

        assertEquals(TextExtractionResult.EMPTY, result)
    }

    @Test
    fun extract_onProcessThrow_returnsEmpty() = runTest {
        val extractor = TextExtractor.createForTest {
            throw RuntimeException("corrupt pipeline")
        }

        val result = extractor.extract(bitmap)

        assertTrue(result.fullText.isEmpty())
        assertTrue(result.blocks.isEmpty())
    }
}
