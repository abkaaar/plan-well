package com.planwell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.planwell.app.domain.model.Priority
import com.planwell.app.ui.theme.AccentGreen
import com.planwell.app.ui.theme.DangerRed
import com.planwell.app.ui.theme.TextFaint
import com.planwell.app.ui.theme.WarningAmber

@Composable
fun PriorityChip(
    priority: Priority,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val (label, color) = when (priority) {
        Priority.HIGH -> "High" to DangerRed
        Priority.MEDIUM -> "Medium" to WarningAmber
        Priority.LOW -> "Low" to AccentGreen
        Priority.NONE -> "None" to TextFaint
    }
    AssistChip(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        label = {
            Text(label, style = MaterialTheme.typography.labelSmall)
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
        },
    )
}

fun priorityDotColor(priority: Priority) = when (priority) {
    Priority.HIGH -> DangerRed
    Priority.MEDIUM -> WarningAmber
    Priority.LOW -> AccentGreen
    Priority.NONE -> TextFaint
}
