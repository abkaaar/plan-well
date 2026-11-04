package com.planwell.app.data.extract

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.planwell.app.domain.extract.DateTimeRanker
import com.planwell.app.domain.extract.EntityType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HeuristicEntityExtractorDateTimeTest {

    private lateinit var extractor: HeuristicEntityExtractor
    private val zone = ZoneId.of("Africa/Lagos")

    /** Fixed: Monday 10 Nov 2025 12:00 Lagos */
    private val reference: ZonedDateTime =
        ZonedDateTime.of(2025, 11, 10, 12, 0, 0, 0, zone)
    private val referenceMs = reference.toInstant().toEpochMilli()

    @Before
    fun setUp() {
        extractor = HeuristicEntityExtractor(PhoneNumberUtil.getInstance()).also {
            it.zoneOverride = zone
        }
    }

    private fun dateMs(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute), zone)
            .toInstant()
            .toEpochMilli()

    private suspend fun firstDateMs(text: String): Long? =
        extractor.extract(text, referenceMs)
            .firstOrNull { it.type == EntityType.DATE_TIME }
            ?.resolvedTimestampMs

    @Test
    fun namedMonth_dayFirst() = runTest {
        assertEquals(dateMs(2025, 12, 14), firstDateMs("Meetup 14 Dec"))
    }

    @Test
    fun namedMonth_monthFirst() = runTest {
        assertEquals(dateMs(2025, 12, 14), firstDateMs("Meetup Dec 14"))
    }

    @Test
    fun namedMonth_fullWithOrdinal() = runTest {
        assertEquals(dateMs(2025, 12, 14), firstDateMs("December 14th"))
    }

    @Test
    fun namedMonth_weekdayPrefix() = runTest {
        assertEquals(dateMs(2025, 12, 14), firstDateMs("SAT 14 DEC"))
        assertEquals(dateMs(2025, 12, 14), firstDateMs("Sat, Dec 14"))
    }

    @Test
    fun numeric_ddMmYyyy() = runTest {
        assertEquals(dateMs(2026, 12, 14), firstDateMs("On 14/12/2026"))
    }

    @Test
    fun numeric_ddMmYy() = runTest {
        assertEquals(dateMs(2026, 12, 14), firstDateMs("On 14-12-26"))
    }

    @Test
    fun numeric_iso() = runTest {
        assertEquals(dateMs(2026, 12, 14), firstDateMs("On 2026-12-14"))
    }

    @Test
    fun numeric_dayGt12_disambiguates() = runTest {
        // 25/03/2026 → day 25 month 3
        assertEquals(dateMs(2026, 3, 25), firstDateMs("Due 25/03/2026"))
    }

    @Test
    fun times_attachToDate() = runTest {
        assertEquals(dateMs(2025, 12, 14, 19, 0), firstDateMs("Show 14 Dec 7PM"))
        assertEquals(dateMs(2025, 12, 14, 19, 30), firstDateMs("Show 14 Dec 7:30pm"))
        assertEquals(dateMs(2025, 12, 14, 19, 30), firstDateMs("Show 14 Dec 7.30 pm"))
        assertEquals(dateMs(2025, 12, 14, 19, 0), firstDateMs("Show 14 Dec 19:00"))
    }

    @Test
    fun doorsOpen_time() = runTest {
        val ms = firstDateMs("Concert 14 Dec doors open 6:30")
        assertEquals(dateMs(2025, 12, 14, 6, 30), ms)
    }

    @Test
    fun relative_phrases() = runTest {
        assertEquals(dateMs(2025, 11, 10), firstDateMs("due today"))
        assertEquals(dateMs(2025, 11, 10, 20, 0), firstDateMs("party tonight"))
        assertEquals(dateMs(2025, 11, 11), firstDateMs("see you tomorrow"))
        assertEquals(dateMs(2025, 11, 14), firstDateMs("next Friday")) // Fri 14 Nov 2025
        assertEquals(dateMs(2025, 11, 15), firstDateMs("this weekend")) // Sat
        assertEquals(dateMs(2025, 11, 13), firstDateMs("in 3 days"))
        assertEquals(dateMs(2025, 11, 17), firstDateMs("next week"))
    }

    @Test
    fun deadline_phrasing() = runTest {
        assertEquals(dateMs(2025, 11, 14), firstDateMs("submit by Friday"))
        assertNotNull(firstDateMs("before 31st"))
        assertEquals(dateMs(2025, 12, 31), firstDateMs("deadline: 31 Dec"))
        // Reference is Monday → "due Monday" resolves to today
        assertEquals(dateMs(2025, 11, 10), firstDateMs("due Monday"))
    }

    @Test
    fun bare_ordinal() = runTest {
        // the 5th from Nov 10 → Dec 5 2025
        assertEquals(dateMs(2025, 12, 5), firstDateMs("Pay on the 5th"))
    }

    @Test
    fun range_takesStart() = runTest {
        assertEquals(dateMs(2025, 12, 14), firstDateMs("Festival 14–16 Dec"))
        assertEquals(dateMs(2025, 12, 14), firstDateMs("Festival 14-16 Dec"))
    }

    @Test
    fun ranking_prefersDeadlineOverPostedDate() = runTest {
        val text = "Posted Jan 3. Register by Dec 31 for the cohort."
        val entities = extractor.extract(text, referenceMs)
        val ranked = DateTimeRanker.rankDateTimes(entities, referenceMs, text)
        assertNotNull(ranked)
        assertEquals(dateMs(2025, 12, 31), ranked!!.resolvedTimestampMs)
    }

    @Test
    fun noYear_rollsToNextFuture() = runTest {
        // Jan 3 from Nov 10 2025 → Jan 3 2026
        assertEquals(dateMs(2026, 1, 3), firstDateMs("Posted Jan 3"))
    }

    @Test
    fun defaultTime_isNineAm() = runTest {
        val entity = extractor.extract("Meetup 14 Dec", referenceMs)
            .first { it.type == EntityType.DATE_TIME }
        assertEquals("time_default_09", entity.normalised)
        assertEquals(dateMs(2025, 12, 14, 9, 0), entity.resolvedTimestampMs)
    }
}
