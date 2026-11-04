package com.planwell.app.data.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextExtractorInstrumentedTest {

    private lateinit var managed: ManagedTextRecognizer
    private lateinit var extractor: TextExtractor

    @Before
    fun setUp() {
        managed = ManagedTextRecognizer()
        extractor = TextExtractor(managed)
    }

    @After
    fun tearDown() {
        managed.close()
    }

    @Test
    fun extract_fromBundledAsset_findsKnownStrings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("ocr_sample.png").use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        requireNotNull(bitmap) { "ocr_sample.png missing or undecodable" }

        val result = try {
            extractor.extract(bitmap)
        } finally {
            bitmap.recycle()
        }

        val haystack = result.fullText.lowercase()
        assertFalse("expected OCR text", haystack.isBlank())
        assertTrue("missing PlanWell OCR Fixture in: ${result.fullText}", "planwell" in haystack)
        assertTrue("missing date in: ${result.fullText}", "march" in haystack || "2026" in haystack)
        assertTrue("missing City Hall in: ${result.fullText}", "city" in haystack && "hall" in haystack)
    }

    @Test
    fun extract_corruptBitmap_returnsEmptyWithoutCrashing() = runBlocking {
        // 1x1 blank bitmap has no readable text; extractor must not throw.
        val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        try {
            extractor.extract(blank)
        } finally {
            blank.recycle()
        }

        val recycled = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        recycled.recycle()
        val empty = extractor.extract(recycled)
        assertTrue(empty.fullText.isEmpty())
        assertTrue(empty.blocks.isEmpty())
    }
}
