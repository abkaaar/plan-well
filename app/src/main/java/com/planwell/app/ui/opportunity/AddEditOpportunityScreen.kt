package com.planwell.app.ui.opportunity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.SourcePlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOpportunityScreen(
    onDone: (Long?) -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditOpportunityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMore by remember { mutableStateOf(uiState.isEditing) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone(uiState.savedId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit opportunity" else "New opportunity")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null && uiState.title.isBlank(),
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            OutlinedTextField(
                value = uiState.sourceUrl,
                onValueChange = viewModel::onSourceUrlChange,
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Type", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                OpportunityType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = {
                            Text(
                                type.name.lowercase().replaceFirstChar { it.titlecase() },
                            )
                        },
                    )
                }
            }

            TextButton(onClick = { showMore = !showMore }) {
                Text(if (showMore) "Hide details" else "More details")
            }

            if (showMore) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    listOf(
                        OpportunityStatus.NEEDS_REVIEW,
                        OpportunityStatus.ACTIVE,
                        OpportunityStatus.COMPLETED,
                    ).forEach { status ->
                        FilterChip(
                            selected = uiState.status == status,
                            onClick = { viewModel.onStatusChange(status) },
                            label = {
                                Text(
                                    when (status) {
                                        OpportunityStatus.NEEDS_REVIEW -> "Needs review"
                                        OpportunityStatus.ACTIVE -> "Active"
                                        OpportunityStatus.COMPLETED -> "Completed"
                                        OpportunityStatus.ARCHIVED -> "Archived"
                                    },
                                )
                            },
                        )
                    }
                }

                Text("Source platform", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Inferred from URL when possible (${uiState.sourcePlatform.label()}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    SourcePlatform.entries.forEach { platform ->
                        FilterChip(
                            selected = uiState.sourcePlatform == platform,
                            onClick = { viewModel.onSourcePlatformChange(platform) },
                            label = { Text(platform.label()) },
                        )
                    }
                }

                if (uiState.categories.isNotEmpty()) {
                    Text("Category", style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        FilterChip(
                            selected = uiState.categoryId == null,
                            onClick = { viewModel.onCategoryChange(null) },
                            label = { Text("None") },
                        )
                        uiState.categories.forEach { category ->
                            FilterChip(
                                selected = uiState.categoryId == category.id,
                                onClick = { viewModel.onCategoryChange(category.id) },
                                label = { Text(category.name) },
                            )
                        }
                    }
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
