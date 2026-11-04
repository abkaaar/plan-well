package com.planwell.app.ui.opportunity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityInboxScreen(
    onBack: () -> Unit,
    onOpenOpportunity: (Long) -> Unit,
    onAddOpportunityManual: () -> Unit,
    onOpenExtraction: (Long) -> Unit,
    viewModel: OpportunityInboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCaptureSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OpportunityInboxEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted “${event.title}”",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.id)
                    }
                }
                is OpportunityInboxEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCaptureSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Capture opportunity")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search opportunities…") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpportunityListFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.statusFilter == filter,
                        onClick = { viewModel.onStatusFilterChange(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    OpportunityListFilter.INBOX -> "Inbox"
                                    OpportunityListFilter.NEEDS_REVIEW -> "Needs review"
                                    OpportunityListFilter.ACTIVE -> "Active"
                                    OpportunityListFilter.COMPLETED -> "Completed"
                                    OpportunityListFilter.ARCHIVED -> "Archived"
                                    OpportunityListFilter.TRASH -> "Trash"
                                },
                            )
                        },
                    )
                }
            }

            if (uiState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.categoryId == null,
                        onClick = { viewModel.onCategoryFilterChange(null) },
                        label = { Text("All categories") },
                    )
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = uiState.categoryId == category.id,
                            onClick = { viewModel.onCategoryFilterChange(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Loading…")
                    }
                }
                uiState.error != null -> {
                    EmptyState(
                        title = "Something went wrong",
                        message = uiState.error ?: "",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.items.isEmpty() -> {
                    EmptyState(
                        title = when (uiState.statusFilter) {
                            OpportunityListFilter.TRASH -> "Trash is empty"
                            OpportunityListFilter.ARCHIVED -> "No archived items"
                            else -> "Nothing saved"
                        },
                        message = when (uiState.statusFilter) {
                            OpportunityListFilter.TRASH -> "Deleted items appear here"
                            OpportunityListFilter.ARCHIVED -> "Archive from detail"
                            else -> "Share here or tap +"
                        },
                        actionLabel = if (uiState.statusFilter == OpportunityListFilter.INBOX) {
                            "Save"
                        } else {
                            null
                        },
                        onAction = if (uiState.statusFilter == OpportunityListFilter.INBOX) {
                            { showCaptureSheet = true }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.items, key = { it.id }) { opportunity ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OpportunityCard(
                                    opportunity = opportunity,
                                    categoryName = opportunity.categoryId?.let {
                                        uiState.categoryNames[it]
                                    },
                                    onClick = { onOpenOpportunity(opportunity.id) },
                                )
                                if (uiState.statusFilter == OpportunityListFilter.TRASH) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.restore(opportunity.id) }) {
                                            Text("Restore")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.permanentlyDelete(opportunity.id)
                                            },
                                        ) {
                                            Text("Delete forever")
                                        }
                                    }
                                } else if (uiState.statusFilter != OpportunityListFilter.ARCHIVED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = { viewModel.archive(opportunity) },
                                        ) {
                                            Text("Archive")
                                        }
                                        TextButton(
                                            onClick = { viewModel.delete(opportunity) },
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
            onError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }
}
