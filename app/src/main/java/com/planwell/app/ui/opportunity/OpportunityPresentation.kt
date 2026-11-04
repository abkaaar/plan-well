package com.planwell.app.ui.opportunity

import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.SourcePlatform
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class OpportunityListFilter {
    INBOX,
    NEEDS_REVIEW,
    ACTIVE,
    COMPLETED,
    ARCHIVED,
    TRASH,
}

enum class DeadlineUrgency {
    NONE,
    UPCOMING,
    OVERDUE,
}

/** Confirmed application deadline, else confirmed event date — never invented. */
fun Opportunity.confirmedDeadlineMs(): Long? {
    val deadline = dates.firstOrNull {
        it.type == OpportunityDateType.APPLICATION_DEADLINE && it.isUserConfirmed && it.dateMs != null
    }?.dateMs
    if (deadline != null) return deadline
    return dates.firstOrNull {
        it.type == OpportunityDateType.EVENT && it.isUserConfirmed && it.dateMs != null
    }?.dateMs
}

fun deadlineUrgency(deadlineMs: Long?, nowMs: Long = System.currentTimeMillis()): DeadlineUrgency {
    if (deadlineMs == null) return DeadlineUrgency.NONE
    if (deadlineMs < nowMs) return DeadlineUrgency.OVERDUE
    val weekMs = TimeUnit.DAYS.toMillis(7)
    return if (deadlineMs - nowMs <= weekMs) DeadlineUrgency.UPCOMING else DeadlineUrgency.NONE
}

fun Opportunity.statusLabel(): String = when (status) {
    OpportunityStatus.NEEDS_REVIEW -> "Needs review"
    OpportunityStatus.ACTIVE -> "Active"
    OpportunityStatus.COMPLETED -> "Completed"
    OpportunityStatus.ARCHIVED -> "Archived"
}

fun SourcePlatform.label(): String = when (this) {
    SourcePlatform.INSTAGRAM -> "Instagram"
    SourcePlatform.BROWSER -> "Browser"
    SourcePlatform.OTHER -> "Other"
    SourcePlatform.UNKNOWN -> "Unknown"
}

fun OpportunityDate.typeLabel(): String = when (type) {
    OpportunityDateType.APPLICATION_DEADLINE -> "Application deadline"
    OpportunityDateType.EVENT -> "Event date"
    OpportunityDateType.OTHER -> label?.takeIf { it.isNotBlank() } ?: "Other date"
}

fun formatOpportunityDate(ms: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))

fun formatOpportunityDateTime(ms: Long): String =
    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(ms))

/** Host from URL when parseable; otherwise null (do not invent). */
fun sourceHostOrNull(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return runCatching {
        val normalized = if (url.contains("://")) url else "https://$url"
        URI(normalized).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

fun Opportunity.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return title.contains(q, ignoreCase = true) ||
        (description?.contains(q, ignoreCase = true) == true) ||
        (sourceUrl?.contains(q, ignoreCase = true) == true)
}
