package com.planwell.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import com.planwell.app.ui.category.parseHexColor
import com.planwell.app.ui.components.EmptyState
import com.planwell.app.ui.components.TaskCard
import com.planwell.app.ui.theme.AccentGreen
import com.planwell.app.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())
    }

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
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenCategories) {
                        Icon(Icons.Outlined.Label, contentDescription = "Categories")
                    }
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Trash")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export tasks (JSON)") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.exportTasks()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
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
            uiState.tasks.isEmpty() -> {
                EmptyState(
                    title = "Nothing planned yet",
                    message = "Tap + to add your first task",
                    actionLabel = "Add task",
                    onAction = onAddTask,
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
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp),
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
                    item {
                        Text(
                            text = "${uiState.filteredTasks.count { !it.isCompleted }} active · " +
                                "${uiState.filteredTasks.size} shown",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (uiState.filteredTasks.isEmpty()) {
                        item {
                            Text(
                                "No tasks in this category",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(uiState.filteredTasks, key = { it.id }) { task ->
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
    }
}

@Composable
private fun TodayProgressHeader(stats: CompletionStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompletionRing(
            progress = stats.progress,
            modifier = Modifier.size(56.dp),
        )
        Column {
            Text(
                text = "${stats.todayCompleted} of ${stats.todayTotal} due today",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (stats.todayTotal == 0) {
                    "No tasks scheduled for today"
                } else {
                    "${(stats.progress * 100).toInt()}% complete"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletionRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.height(56.dp)) {
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
