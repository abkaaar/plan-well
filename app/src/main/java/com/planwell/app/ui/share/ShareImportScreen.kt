package com.planwell.app.ui.share

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.planwell.app.domain.classify.EntryKind
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ShareImportScreen(
    state: ShareImportState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onKindChange: (EntryKind) -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDueChange: (Long?) -> Unit,
    onClearPhone: () -> Unit,
    onClearAddress: () -> Unit,
    onClearUrl: () -> Unit,
    onClearMoney: () -> Unit,
) {
    when (state) {
        is ShareImportState.Loading -> LoadingPane(state.stage, onCancel)
        is ShareImportState.Saving -> LoadingPane("Saving…", onCancel = null)
        is ShareImportState.Saved -> LoadingPane("Saved", onCancel = null)
        is ShareImportState.Error -> ErrorPane(
            message = state.message,
            retryable = state.retryable,
            onCancel = onCancel,
            onRetry = onRetry,
        )
        is ShareImportState.Review -> ReviewPane(
            draft = state.draft,
            onCancel = onCancel,
            onSave = onSave,
            onKindChange = onKindChange,
            onTitleChange = onTitleChange,
            onNotesChange = onNotesChange,
            onDueChange = onDueChange,
            onClearPhone = onClearPhone,
            onClearAddress = onClearAddress,
            onClearUrl = onClearUrl,
            onClearMoney = onClearMoney,
        )
    }
}

@Composable
private fun LoadingPane(stage: String, onCancel: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(stage, style = MaterialTheme.typography.bodyLarge)
        if (onCancel != null) {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ErrorPane(
    message: String,
    retryable: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Couldn’t import", style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            if (retryable) {
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun ReviewPane(
    draft: EditableImportDraft,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onKindChange: (EntryKind) -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDueChange: (Long?) -> Unit,
    onClearPhone: () -> Unit,
    onClearAddress: () -> Unit,
    onClearUrl: () -> Unit,
    onClearMoney: () -> Unit,
) {
    val context = LocalContext.current
    val dateLabel = draft.dueOrStartMs?.let {
        SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Add date & time"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review import", style = MaterialTheme.typography.titleLarge)
        Text(
            "Confidence ${(draft.confidence * 100).toInt()}% — edit before saving",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            EntryKind.entries.forEach { kind ->
                FilterChip(
                    selected = draft.kind == kind,
                    onClick = { onKindChange(kind) },
                    label = {
                        Text(
                            kind.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        )
                    },
                )
            }
        }

        OutlinedTextField(
            value = draft.title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply {
                    draft.dueOrStartMs?.let { timeInMillis = it }
                }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val next = Calendar.getInstance().apply {
                            draft.dueOrStartMs?.let { timeInMillis = it }
                            set(Calendar.YEAR, y)
                            set(Calendar.MONTH, m)
                            set(Calendar.DAY_OF_MONTH, d)
                        }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                next.set(Calendar.HOUR_OF_DAY, hour)
                                next.set(Calendar.MINUTE, minute)
                                next.set(Calendar.SECOND, 0)
                                next.set(Calendar.MILLISECOND, 0)
                                onDueChange(next.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(dateLabel)
        }
        if (draft.dueOrStartMs != null) {
            TextButton(onClick = { onDueChange(null) }) { Text("Clear date") }
        }

        OutlinedTextField(
            value = draft.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        draft.sourceImagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = "Shared image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            draft.phone?.let {
                InputChip(
                    selected = false,
                    onClick = onClearPhone,
                    label = { Text("Phone: $it ✕") },
                )
            }
            draft.url?.let {
                InputChip(
                    selected = false,
                    onClick = onClearUrl,
                    label = { Text("URL ✕") },
                )
            }
            draft.address?.let {
                InputChip(
                    selected = false,
                    onClick = onClearAddress,
                    label = { Text("Place ✕") },
                )
            }
            draft.money?.let {
                InputChip(
                    selected = false,
                    onClick = onClearMoney,
                    label = { Text("Money ✕") },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
        }
    }
}
