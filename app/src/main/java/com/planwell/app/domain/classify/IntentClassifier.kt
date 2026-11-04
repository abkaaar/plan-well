package com.planwell.app.domain.classify

import com.planwell.app.domain.extract.DateTimeRanker
import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.extract.ExtractedEntity
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentClassifier @Inject constructor() {

    fun classify(text: String, entities: List<ExtractedEntity>): Classification {
        val lower = text.lowercase(Locale.US)
        var eventScore = countHits(lower, EVENT_WORDS).toFloat()
        var taskScore = countHits(lower, TASK_WORDS).toFloat()
        var callScore = countHits(lower, CALL_WORDS).toFloat()

        val hasPhone = entities.any { it.type == EntityType.PHONE }
        val hasAddress = entities.any { it.type == EntityType.ADDRESS }
        val dateWithExplicitTime = entities.any {
            it.type == EntityType.DATE_TIME && DateTimeRanker.hasExplicitTime(it.rawText)
        }

        if (hasPhone) callScore += 2f
        if (dateWithExplicitTime && hasAddress) eventScore += 2f

        val max = maxOf(eventScore, taskScore, callScore)
        if (max <= 0f) {
            return Classification(EntryKind.TASK, confidence = 0.4f)
        }

        val kind = when (max) {
            callScore -> EntryKind.CALL
            eventScore -> if (eventScore >= taskScore) EntryKind.EVENT else EntryKind.TASK
            taskScore -> EntryKind.TASK
            else -> EntryKind.TASK
        }

        // Prefer CALL over others when phone boost made it win; EVENT when boosted.
        val resolved = when {
            callScore >= eventScore && callScore >= taskScore && callScore > 0f -> EntryKind.CALL
            eventScore > taskScore && eventScore > 0f -> EntryKind.EVENT
            taskScore > 0f && taskScore >= eventScore -> EntryKind.TASK
            else -> kind
        }

        val total = eventScore + taskScore + callScore
        val winnerScore = when (resolved) {
            EntryKind.CALL -> callScore
            EntryKind.EVENT -> eventScore
            EntryKind.TASK -> taskScore
        }
        val confidence = (0.45f + 0.55f * (winnerScore / total)).coerceIn(0.4f, 0.95f)
        return Classification(resolved, confidence)
    }

    private fun countHits(lower: String, words: List<String>): Int =
        words.count { phrase -> lower.contains(phrase) }

    companion object {
        val EVENT_WORDS = listOf(
            "event", "venue", "doors open", "rsvp", "webinar", "conference",
            "ceremony", "service", "match", "concert", "gig", "summit",
        )
        val TASK_WORDS = listOf(
            "deadline", "submit", "apply", "due", "register by", "closes",
            "pay", "renew", "deliver", "expires",
        )
        val CALL_WORDS = listOf(
            "call", "ring", "dial", "phone", "reach out", "follow up with",
        )
    }
}
