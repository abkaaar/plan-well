package com.planwell.app.domain.classify

import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.extract.ExtractedEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IntentClassifierTest(
    private val text: String,
    private val expected: EntryKind,
    private val extraEntities: List<ExtractedEntity>,
) {
    private val classifier = IntentClassifier()

    @Test
    fun classifies() {
        val result = classifier.classify(text, extraEntities)
        assertEquals(expected, result.kind)
        assertTrue(result.confidence in 0.4f..1f)
    }

    companion object {
        private fun phone() = ExtractedEntity(EntityType.PHONE, "0803", 0, 4, normalised = "+234803")
        private fun address() = ExtractedEntity(EntityType.ADDRESS, "City Hall", 0, 9)
        private fun timedDate() = ExtractedEntity(
            EntityType.DATE_TIME,
            "14 Dec 7PM",
            0,
            10,
            resolvedTimestampMs = 1L,
            normalised = "time_explicit",
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{1}: {0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("Submit your application before the deadline", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Register by Friday to apply for the grant", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Pay the fee and renew your licence", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Deliver the documents when they are due", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Applications closes soon — expires midnight", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Doors open 6pm — RSVP for the conference", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Join the webinar and summit next week", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Wedding ceremony and thanksgiving service", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Football match at the stadium venue", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Concert gig tonight — get tickets", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Please call me about the invoice", EntryKind.CALL, emptyList<ExtractedEntity>()),
            arrayOf("Ring the office or dial the desk line", EntryKind.CALL, emptyList<ExtractedEntity>()),
            arrayOf("Reach out and follow up with the recruiter", EntryKind.CALL, emptyList<ExtractedEntity>()),
            arrayOf("Phone interview scheduled", EntryKind.CALL, emptyList<ExtractedEntity>()),
            arrayOf(
                "Talk tomorrow",
                EntryKind.CALL,
                listOf(phone()),
            ),
            arrayOf(
                "Meetup 14 Dec 7PM at City Hall",
                EntryKind.EVENT,
                listOf(timedDate(), address()),
            ),
            arrayOf("Something vague without keywords", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Apply now — deadline is soon", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("Community event at the plaza", EntryKind.EVENT, emptyList<ExtractedEntity>()),
            arrayOf("Please dial +2348031234567", EntryKind.CALL, listOf(phone())),
            arrayOf("Submit deliverable before it expires", EntryKind.TASK, emptyList<ExtractedEntity>()),
            arrayOf("RSVP for the ceremony at the hotel", EntryKind.EVENT, emptyList<ExtractedEntity>()),
        )
    }
}
