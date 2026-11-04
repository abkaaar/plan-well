package com.planwell.app.domain.capture

import com.planwell.app.domain.model.SourcePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedContentParserTest {

    @Test
    fun emptyShareUsesFallbackTitle() {
        val parsed = SharedContentParser.parse(null, null)
        assertEquals("Shared item", parsed.title)
        assertNull(parsed.sourceUrl)
        assertEquals(SourcePlatform.UNKNOWN, parsed.sourcePlatform)
    }

    @Test
    fun extractsInstagramUrlAndPlatform() {
        val parsed = SharedContentParser.parse(
            subject = "Check this",
            text = "https://www.instagram.com/p/abc123/",
        )
        assertEquals("Check this", parsed.title)
        assertEquals("https://www.instagram.com/p/abc123/", parsed.sourceUrl)
        assertEquals(SourcePlatform.INSTAGRAM, parsed.sourcePlatform)
    }

    @Test
    fun urlOnlyUsesHostAsTitle() {
        val parsed = SharedContentParser.parse(
            subject = null,
            text = "https://example.com/jobs/123",
        )
        assertEquals("example.com", parsed.title)
        assertEquals(SourcePlatform.BROWSER, parsed.sourcePlatform)
    }

    @Test
    fun wwwUrlIsNormalized() {
        val url = SharedContentParser.extractFirstUrl("See www.example.org/path now")
        assertEquals("https://www.example.org/path", url)
    }

    @Test
    fun malformedTextStillProducesDraft() {
        val parsed = SharedContentParser.parse("   ", "!!!")
        assertTrue(parsed.title.isNotBlank())
    }
}
