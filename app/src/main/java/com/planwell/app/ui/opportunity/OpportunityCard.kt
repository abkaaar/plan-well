package com.planwell.app.ui.opportunity

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.SourcePlatform
import com.planwell.app.ui.theme.DangerRed
import com.planwell.app.ui.theme.WarningAmber

@Composable
fun OpportunityCard(
    opportunity: Opportunity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    nowMs: Long = System.currentTimeMillis(),
) {
    val deadlineMs = opportunity.confirmedDeadlineMs()
    val urgency = deadlineUrgency(deadlineMs, nowMs)
    val needsReview = opportunity.status == OpportunityStatus.NEEDS_REVIEW
    val archived = opportunity.status == OpportunityStatus.ARCHIVED || opportunity.isDeleted
    val hasUnconfirmedDate = opportunity.dates.any { !it.isUserConfirmed }

    val borderColor = when {
        urgency == DeadlineUrgency.OVERDUE -> DangerRed
        needsReview -> WarningAmber
        urgency == DeadlineUrgency.UPCOMING -> WarningAmber.copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    val primaryChip = when (urgency) {
        DeadlineUrgency.OVERDUE -> "Overdue"
        DeadlineUrgency.UPCOMING -> "Due soon"
        DeadlineUrgency.NONE -> opportunity.statusLabel().takeIf {
            opportunity.status != OpportunityStatus.ACTIVE
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (archived) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = opportunity.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (primaryChip != null) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(primaryChip) },
                    enabled = false,
                )
            }
            if (hasUnconfirmedDate) {
                Text(
                    text = "Confirm date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val source = categoryName?.takeIf { it.isNotBlank() }
                ?: sourceHostOrNull(opportunity.sourceUrl)
                ?: opportunity.sourcePlatform.label().takeIf {
                    opportunity.sourcePlatform != SourcePlatform.UNKNOWN
                }
            val meta = buildList {
                source?.let { add(it) }
                deadlineMs?.let { add(formatOpportunityDate(it)) }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
