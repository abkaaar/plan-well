package com.planwell.app.domain.usecase.opportunity

import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateOpportunityFieldsTest {

    @Test
    fun oversizedSourceUrlRejected() {
        val error = runCatching {
            validateOpportunityFields(
                title = "Ok",
                description = null,
                sourceUrl = "https://example.com/" + "x".repeat(2_100),
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun blankSourceUrlAllowedAsNullTrimmedByUseCase() {
        // Field validator only checks length when non-null; blank is caller's concern after trim.
        validateOpportunityFields("Title", null, null)
    }

    @Test
    fun oversizedDescriptionRejected() {
        val error = runCatching {
            validateOpportunityFields("Title", "d".repeat(2_001), null)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
