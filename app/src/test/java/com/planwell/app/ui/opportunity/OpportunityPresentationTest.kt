package com.planwell.app.ui.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpportunityPresentationTest {

    @Test
    fun confirmedDeadlinePrefersApplicationDeadline() {
        val opportunity = Opportunity(
            title = "x",
            dates = listOf(
                OpportunityDate(
                    type = OpportunityDateType.EVENT,
                    dateMs = 100L,
                    isUserConfirmed = true,
                ),
                OpportunityDate(
                    type = OpportunityDateType.APPLICATION_DEADLINE,
                    dateMs = 200L,
                    isUserConfirmed = true,
                ),
            ),
        )
        assertEquals(200L, opportunity.confirmedDeadlineMs())
    }

    @Test
    fun unconfirmedDatesAreNotInventedAsDeadline() {
        val opportunity = Opportunity(
            title = "x",
            dates = listOf(
                OpportunityDate(
                    type = OpportunityDateType.APPLICATION_DEADLINE,
                    extractedDateMs = 999L,
                    isUserConfirmed = false,
                ),
            ),
        )
        assertNull(opportunity.confirmedDeadlineMs())
    }

    @Test
    fun deadlineUrgencyOverdueAndUpcoming() {
        val now = 1_000_000L
        assertEquals(DeadlineUrgency.OVERDUE, deadlineUrgency(now - 1, now))
        assertEquals(DeadlineUrgency.UPCOMING, deadlineUrgency(now + 86_400_000L, now))
        assertEquals(DeadlineUrgency.NONE, deadlineUrgency(null, now))
    }

    @Test
    fun sourceHostParsesKnownUrl() {
        assertEquals("instagram.com", sourceHostOrNull("https://www.instagram.com/p/abc"))
        assertNull(sourceHostOrNull(null))
        assertNull(sourceHostOrNull(""))
    }
}
