package com.planwell.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class HomeFabAction {
    ADD_TASK,
    SCHEDULE_CALL,
    ADD_EVENT,
    SAVE_OPPORTUNITY,
}

@Composable
fun ExpandableHomeFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (HomeFabAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = expanded) {
        onExpandedChange(false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onExpandedChange(false) },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FabMenuItem(
                        label = "Task",
                        contentDescription = "Add task",
                        icon = Icons.Filled.TaskAlt,
                        onClick = {
                            onExpandedChange(false)
                            onAction(HomeFabAction.ADD_TASK)
                        },
                    )
                    FabMenuItem(
                        label = "Call",
                        contentDescription = "Schedule a call",
                        icon = Icons.Filled.Phone,
                        onClick = {
                            onExpandedChange(false)
                            onAction(HomeFabAction.SCHEDULE_CALL)
                        },
                    )
                    FabMenuItem(
                        label = "Event",
                        contentDescription = "Add event",
                        icon = Icons.Filled.Event,
                        onClick = {
                            onExpandedChange(false)
                            onAction(HomeFabAction.ADD_EVENT)
                        },
                    )
                    FabMenuItem(
                        label = "Save",
                        contentDescription = "Save opportunity",
                        icon = Icons.Filled.Inbox,
                        onClick = {
                            onExpandedChange(false)
                            onAction(HomeFabAction.SAVE_OPPORTUNITY)
                        },
                    )
                }
            }

            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.semantics {
                    contentDescription = if (expanded) "Close quick actions" else "Open quick actions"
                },
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun FabMenuItem(
    label: String,
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            Icon(icon, contentDescription = null)
        }
    }
}
