package com.planwell.app.ui.opportunity

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.domain.model.ExtractionCandidateKind
import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.ui.theme.WarningAmber
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityExtractionReviewScreen(
    onDone: (opportunityId: Long, openDetailForAct: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: OpportunityExtractionReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            onDone(viewModel.opportunityId, uiState.openDetailForAct)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review extraction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading || uiState.isRunning -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            if (uiState.isRunning) "Extracting text on device…" else "Loading…",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val status = uiState.opportunity?.extractionStatus
                    if (status == ExtractionStatus.FAILED || status == ExtractionStatus.SKIPPED) {
                        Text(
                            uiState.error ?: "Extraction unavailable",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Your opportunity and any attached images are unchanged. " +
                                "You can continue with manual entry.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = viewModel::skipToManual) {
                            Text("Continue manually")
                        }
                        OutlinedButton(onClick = { viewModel.runExtractionIfNeeded(force = true) }) {
                            Text("Try again")
                        }
                        return@Column
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Text(
                        "Confirm each suggestion before it is saved. Nothing is confirmed automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val raw = uiState.opportunity?.extractedRawText
                    if (!raw.isNullOrBlank()) {
                        TextButton(onClick = viewModel::toggleRawText) {
                            Text(if (uiState.rawTextExpanded) "Hide OCR text" else "Show OCR text")
                        }
                        if (uiState.rawTextExpanded) {
                            Text(
                                raw,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text("Text candidates", style = MaterialTheme.typography.titleMedium)
                    if (uiState.candidateRows.isEmpty()) {
                        Text(
                            "No text suggestions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.candidateRows.forEach { row ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    candidateKindLabel(row.candidate.kind),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                OutlinedTextField(
                                    value = row.editedValue,
                                    onValueChange = {
                                        viewModel.onCandidateEdit(row.candidate.id, it)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = row.accepted != false,
                                )
                                row.candidate.sourceSnippet?.let {
                                    Text(
                                        "From: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = row.accepted == true,
                                        onClick = {
                                            viewModel.onCandidateAccept(row.candidate.id, true)
                                        },
                                        label = { Text("Accept") },
                                    )
                                    FilterChip(
                                        selected = row.accepted == false,
                                        onClick = {
                                            viewModel.onCandidateAccept(row.candidate.id, false)
                                        },
                                        label = { Text("Reject") },
                                    )
                                }
                            }
                        }
                    }

                    Text("Date candidates", style = MaterialTheme.typography.titleMedium)
                    if (uiState.dateRows.isEmpty()) {
                        Text(
                            "No date suggestions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.dateRows.forEach { row ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(row.date.typeLabel(), style = MaterialTheme.typography.titleSmall)
                                when {
                                    row.confirmedDateMs != null -> {
                                        Text(formatOpportunityDate(row.confirmedDateMs))
                                    }
                                    else -> {
                                        Text(
                                            row.date.label?.let { "Ambiguous: $it — pick a date to confirm" }
                                                ?: "No parseable date — pick manually to confirm",
                                            color = WarningAmber,
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = row.accepted == true,
                                        onClick = {
                                            if (row.confirmedDateMs != null) {
                                                viewModel.onDateAccept(row.date.id, true)
                                            } else {
                                                pickDate(context, null) { ms ->
                                                    viewModel.onDatePicked(row.date.id, ms)
                                                }
                                            }
                                        },
                                        label = { Text("Confirm") },
                                    )
                                    TextButton(
                                        onClick = {
                                            pickDate(context, row.confirmedDateMs) { ms ->
                                                viewModel.onDatePicked(row.date.id, ms)
                                            }
                                        },
                                    ) {
                                        Text("Edit date")
                                    }
                                    FilterChip(
                                        selected = row.accepted == false,
                                        onClick = {
                                            viewModel.onDateAccept(row.date.id, false)
                                        },
                                        label = { Text("Reject") },
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.applyAndFinish(openDetailForAct = true) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (uiState.isSaving) "Saving…"
                            else "Apply & continue to create task",
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.applyAndFinish(openDetailForAct = false) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Apply accepted only")
                    }
                    OutlinedButton(
                        onClick = viewModel::skipToManual,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Skip — keep as-is")
                    }
                }
            }
        }
    }
}

private fun candidateKindLabel(kind: ExtractionCandidateKind): String = when (kind) {
    ExtractionCandidateKind.TITLE -> "Title"
    ExtractionCandidateKind.ORGANIZATION -> "Organization"
    ExtractionCandidateKind.URL -> "Source URL"
    ExtractionCandidateKind.ELIGIBILITY -> "Eligibility"
}

private fun pickDate(
    context: android.content.Context,
    initialMs: Long?,
    onPicked: (Long) -> Unit,
) {
    val cal = Calendar.getInstance().apply {
        initialMs?.let { timeInMillis = it }
    }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            val next = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPicked(next.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}
