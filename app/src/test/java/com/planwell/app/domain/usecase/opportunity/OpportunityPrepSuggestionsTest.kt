package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityType
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityPrepSuggestionsTest {

    @Test
    fun deadlineTypeSuggestsApplicationSteps() {
        val steps = OpportunityPrepSuggestions.forOpportunity(
            Opportunity(title = "Grant", type = OpportunityType.DEADLINE),
        )
        assertTrue(steps.any { it.contains("documents", ignoreCase = true) })
        assertTrue(steps.any { it.contains("Submit", ignoreCase = true) })
    }

    @Test
    fun confirmedEventSuggestsAttendanceSteps() {
        val steps = OpportunityPrepSuggestions.forOpportunity(
            Opportunity(
                title = "Fair",
                type = OpportunityType.EVENT,
                dates = listOf(
                    OpportunityDate(
                        type = OpportunityDateType.EVENT,
                        dateMs = 1L,
                        isUserConfirmed = true,
                    ),
                ),
            ),
        )
        assertTrue(steps.any { it.contains("calendar", ignoreCase = true) })
    }

    @Test
    fun otherTypeStillOffersGenericSteps() {
        val steps = OpportunityPrepSuggestions.forOpportunity(
            Opportunity(title = "Note", type = OpportunityType.OTHER),
        )
        assertTrue(steps.isNotEmpty())
    }
}
