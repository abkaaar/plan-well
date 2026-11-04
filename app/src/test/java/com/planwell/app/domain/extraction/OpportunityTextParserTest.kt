package com.planwell.app.domain.extraction

import com.planwell.app.domain.model.ExtractionCandidateKind
import com.planwell.app.domain.model.OpportunityDateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityTextParserTest {

    @Test
    fun blankTextReturnsEmpty() {
        val parsed = OpportunityTextParser.parse("   ")
        assertTrue(parsed.textCandidates.isEmpty())
        assertTrue(parsed.dateCandidates.isEmpty())
    }

    @Test
    fun extractsUrlAndNormalizesWww() {
        val parsed = OpportunityTextParser.parse(
            "Career Fair\nApply at www.example.com/apply\n",
        )
        val url = parsed.textCandidates.firstOrNull { it.kind == ExtractionCandidateKind.URL }
        assertNotNull(url)
        assertEquals("https://www.example.com/apply", url!!.valueText)
    }

    @Test
    fun deadlineKeywordClassifiesDate() {
        val parsed = OpportunityTextParser.parse(
            "Scholarship\nApplication deadline: March 15, 2026\n",
        )
        assertTrue(
            parsed.dateCandidates.any {
                it.type == OpportunityDateType.APPLICATION_DEADLINE && it.extractedDateMs != null
            },
        )
    }

    @Test
    fun eventKeywordClassifiesDate() {
        val parsed = OpportunityTextParser.parse(
            "Workshop on April 2, 2026 at campus hall",
        )
        assertTrue(
            parsed.dateCandidates.any {
                it.type == OpportunityDateType.EVENT && it.extractedDateMs != null
            },
        )
    }

    @Test
    fun ambiguousSlashDateOmitsEpochMs() {
        val parsed = OpportunityTextParser.parse("Meet on 03/04/2026 somewhere")
        val slash = parsed.dateCandidates.firstOrNull { it.label == "03/04/2026" }
        assertNotNull(slash)
        assertNull(slash!!.extractedDateMs)
        assertEquals(OpportunityDateType.OTHER, slash.type)
    }

    @Test
    fun doesNotInventDatesFromEmptyPoster() {
        val parsed = OpportunityTextParser.parse("Just a title without any dates")
        assertTrue(parsed.dateCandidates.isEmpty())
    }
}
