package com.planwell.app.domain.extract

/**
 * Picks the best DATE_TIME entity for due/start: deadline keywords beat bare
 * dates; among the rest prefer the nearest future timestamp.
 */
object DateTimeRanker {

    private val deadlineKeywords = listOf(
        "deadline",
        "register by",
        "apply by",
        "due",
        "closes",
        "closing",
        "submit by",
        "before",
        "expires",
        "by ",
    )

    fun rankDateTimes(
        entities: List<ExtractedEntity>,
        referenceTime: Long,
        fullText: String = "",
    ): ExtractedEntity? {
        val dates = entities.filter {
            it.type == EntityType.DATE_TIME && it.resolvedTimestampMs != null
        }
        if (dates.isEmpty()) return null

        fun hasDeadlineCue(entity: ExtractedEntity): Boolean {
            val start = (entity.start - 48).coerceAtLeast(0)
            val end = (entity.end + 48).coerceAtMost(fullText.length)
            val window = if (fullText.isNotEmpty()) {
                fullText.substring(start, end).lowercase()
            } else {
                entity.rawText.lowercase()
            }
            return deadlineKeywords.any { window.contains(it) }
        }

        val withDeadline = dates.filter { hasDeadlineCue(it) }
        val pool = withDeadline.ifEmpty { dates }

        val future = pool.filter { (it.resolvedTimestampMs ?: 0L) >= referenceTime }
        val candidates = future.ifEmpty { pool }

        return candidates.minByOrNull { entity ->
            val ts = entity.resolvedTimestampMs ?: Long.MAX_VALUE
            kotlin.math.abs(ts - referenceTime)
        }
    }

    private val timeToken = Regex(
        """(?i)\b(?:\d{1,2}([:.]?\d{2})?\s*(?:am|pm)\b|\d{1,2}:\d{2}\b|doors\s+open)""",
    )

    fun hasExplicitTime(rawText: String): Boolean =
        timeToken.containsMatchIn(rawText)
}
