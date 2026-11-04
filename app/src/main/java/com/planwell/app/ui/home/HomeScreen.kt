package com.planwell.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.domain.agenda.AgendaItem
import com.planwell.app.domain.agenda.AgendaItemKind
import com.planwell.app.domain.agenda.AgendaSection
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import com.planwell.app.ui.category.parseHexColor
import com.planwell.app.ui.components.EmptyState
import com.planwell.app.ui.components.ExpandableHomeFab
import com.planwell.app.ui.components.HomeFabAction
import com.planwell.app.ui.components.TaskCard
import com.planwell.app.ui.opportunity.CaptureOpportunitySheet
import com.planwell.app.ui.theme.AccentGreen
import com.planwell.app.ui.theme.DangerRed
import com.planwell.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTask: () -> Unit,
    onScheduleCall: () -> Unit,
    onAddEvent: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onAddOpportunityManual: () -> Unit,
    onOpenOpportunity: (Long) -> Unit,
    onOpenExtraction: (Long) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())
    }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskListEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted “${event.title}”",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.taskId)
                    }
                }
                is TaskListEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today")
                        Text(
                            text = todayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInbox) {
                        Icon(Icons.Outlined.Inbox, contentDescription = "Inbox")
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Categories") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenCategories()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Trash") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenTrash()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.exportTasks()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Loading…")
                }
            }
            uiState.error != null -> {
                EmptyState(
                    title = "Couldn’t load agenda",
                    message = uiState.error ?: "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            uiState.tasks.isEmpty() &&
                uiState.agendaItems.isEmpty() &&
                uiState.needsReview.isEmpty() -> {
                EmptyState(
                    title = "Nothing yet",
                    message = "Tap + to add",
                    actionLabel = "Add",
                    onAction = { fabExpanded = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        TodayProgressHeader(stats = uiState.stats)
                    }
                    if (uiState.needsReview.isNotEmpty()) {
                        item {
                            Text(
                                "Review",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningAmber,
                            )
                        }
                        items(uiState.needsReview, key = { "nr-${it.id}" }) { opp ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenOpportunity(opp.id) },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    opp.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }
                    if (uiState.suggestions.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                uiState.suggestions.forEach { suggestion ->
                                    Text(
                                        suggestion.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = uiState.selectedCategoryId == null,
                                        onClick = { viewModel.selectCategory(null) },
                                        label = { Text("All") },
                                    )
                                }
                                items(uiState.categories, key = { it.id }) { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategoryId == category.id,
                                        onClick = { viewModel.selectCategory(category.id) },
                                        label = { Text(category.name) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        parseHexColor(category.colorHex),
                                                        CircleShape,
                                                    ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    val overdue = uiState.agendaItems.filter { it.section == AgendaSection.OVERDUE }
                    val today = uiState.agendaItems.filter { it.section == AgendaSection.TODAY }

                    if (overdue.isNotEmpty()) {
                        item {
                            Text(
                                "Overdue",
                                style = MaterialTheme.typography.titleMedium,
                                color = DangerRed,
                            )
                        }
                        items(overdue, key = { it.id }) { item ->
                            AgendaItemCard(
                                item = item,
                                timeLabel = timeFormat.format(Date(item.sortTimeMs)),
                                onClick = {
                                    navigateAgendaItem(item, onOpenTask, onOpenOpportunity)
                                },
                            )
                        }
                    }

                    item {
                        Text("Agenda", style = MaterialTheme.typography.titleMedium)
                    }
                    if (today.isEmpty()) {
                        item {
                            Text(
                                "Nothing today",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        items(today, key = { it.id }) { item ->
                            AgendaItemCard(
                                item = item,
                                timeLabel = timeFormat.format(Date(item.sortTimeMs)),
                                onClick = {
                                    navigateAgendaItem(item, onOpenTask, onOpenOpportunity)
                                },
                            )
                        }
                    }

                    item {
                        Text(
                            "Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (uiState.filteredTasks.isEmpty() && uiState.undatedTasks.isEmpty()) {
                        item {
                            Text(
                                "None",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        items(uiState.filteredTasks, key = { "task-list-${it.id}" }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                categoryName = uiState.categoryNames[task.categoryId],
                                onOpen = { onOpenTask(task.id) },
                                onToggleComplete = { viewModel.toggleComplete(task) },
                                onSwipeComplete = { viewModel.toggleComplete(task) },
                                onSwipeDelete = { viewModel.deleteTask(task) },
                            )
                        }
                    }
                    if (uiState.undatedTasks.isNotEmpty()) {
                        item {
                            Text(
                                "No date",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(uiState.undatedTasks, key = { "undated-${it.id}" }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                categoryName = uiState.categoryNames[task.categoryId],
                                onOpen = { onOpenTask(task.id) },
                                onToggleComplete = { viewModel.toggleComplete(task) },
                                onSwipeComplete = { viewModel.toggleComplete(task) },
                                onSwipeDelete = { viewModel.deleteTask(task) },
                            )
                        }
                    }
                }
            }
        }
            ExpandableHomeFab(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                onAction = { action ->
                    when (action) {
                        HomeFabAction.ADD_TASK -> onAddTask()
                        HomeFabAction.SCHEDULE_CALL -> onScheduleCall()
                        HomeFabAction.ADD_EVENT -> onAddEvent()
                        HomeFabAction.SAVE_OPPORTUNITY -> showCaptureSheet = true
                    }
                },
            )
        }
    }

    if (showCaptureSheet) {
        CaptureOpportunitySheet(
            captureSharedContent = viewModel.captureSharedContent,
            onDismiss = { showCaptureSheet = false },
            onManual = onAddOpportunityManual,
            onCaptured = { id, openExtraction ->
                if (openExtraction) onOpenExtraction(id) else onOpenOpportunity(id)
            },
            onError = { viewModel.showMessage(it) },
        )
    }
}

private fun navigateAgendaItem(
    item: AgendaItem,
    onOpenTask: (Long) -> Unit,
    onOpenOpportunity: (Long) -> Unit,
) {
    item.opportunityId?.let {
        onOpenOpportunity(it)
        return
    }
    item.taskId?.let { onOpenTask(it) }
}

@Composable
private fun AgendaItemCard(
    item: AgendaItem,
    timeLabel: String,
    onClick: () -> Unit,
) {
    val accent = when (item.kind) {
        AgendaItemKind.TASK_OVERDUE -> DangerRed
        AgendaItemKind.OPPORTUNITY_DEADLINE -> WarningAmber
        AgendaItemKind.OPPORTUNITY_EVENT -> MaterialTheme.colorScheme.primary
        AgendaItemKind.REMINDER -> MaterialTheme.colorScheme.tertiary
        AgendaItemKind.TASK_DUE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val kindLabel = when (item.kind) {
        AgendaItemKind.TASK_DUE -> "Due"
        AgendaItemKind.TASK_OVERDUE -> "Overdue"
        AgendaItemKind.REMINDER -> "Reminder"
        AgendaItemKind.OPPORTUNITY_DEADLINE -> "Deadline"
        AgendaItemKind.OPPORTUNITY_EVENT -> "Event"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
            SuggestionChip(
                onClick = {},
                enabled = false,
                label = { Text(kindLabel) },
            )
        }
    }
}

@Composable
private fun TodayProgressHeader(stats: CompletionStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompletionRing(
            progress = stats.progress,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = if (stats.todayTotal == 0) {
                "0 due"
            } else {
                "${stats.todayCompleted}/${stats.todayTotal} due"
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun CompletionRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.height(48.dp)) {
        val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    categoryName: String?,
    onOpen: () -> Unit,
    onToggleComplete: () -> Unit,
    onSwipeComplete: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentGreen
                    SwipeToDismissBoxValue.EndToStart -> DangerRed
                    else -> Color.Transparent
                },
                label = "swipeColor",
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Done
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                else -> Icons.Filled.Done
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TaskCard(
            task = task,
            categoryName = categoryName,
            onClick = onOpen,
            onToggleComplete = onToggleComplete,
        )
    }
}
