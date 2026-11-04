package com.planwell.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleExtractorTest {

    private val extractor = TitleExtractor()

    @Test
    fun skipsLeadingHandle() {
        val title = extractor.extract(
            """
            @planwell_ng
            Scholarship Open
            Apply today
            """.trimIndent(),
        )
        assertEquals("Scholarship Open", title)
    }

    @Test
    fun stripsEmojiPrefix() {
        val title = extractor.extract("🔥🔥 LATE APPLICATION WINDOW\nDetails inside")
        assertEquals("LATE APPLICATION WINDOW", title)
    }

    @Test
    fun prefersAllCapsHeadlineUnder60() {
        val title = extractor.extract(
            """
            CALL FOR INTERNS
            Long description that goes on about the programme requirements.
            """.trimIndent(),
        )
        assertEquals("CALL FOR INTERNS", title)
    }

    @Test
    fun stripsTrailingHashtags() {
        val title = extractor.extract("Campus Fair this week #lagos #events #students")
        assertEquals("Campus Fair this week", title)
        assertFalse(title.contains("#"))
    }

    @Test
    fun skipsUrlOnlyLine() {
        val title = extractor.extract(
            """
            https://example.com/post
            Real headline here
            """.trimIndent(),
        )
        assertEquals("Real headline here", title)
    }

    @Test
    fun fallsBackToFirstSentenceWhenBlocksTooLong() {
        val long = "A".repeat(80) + ". Second sentence stays out."
        val title = extractor.extract(long)
        assertTrue(title.length <= 60)
        assertTrue(title.startsWith("A"))
    }
}
