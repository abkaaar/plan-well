package com.planwell.app.ui.opportunity

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.domain.model.AttachmentKind
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.ui.theme.DangerRed
import com.planwell.app.ui.theme.WarningAmber
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenExtractionReview: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    viewModel: OpportunityDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val opportunity = uiState.opportunity
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingReminder by remember { mutableStateOf<PendingReminder?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri)
        val name = uri.lastPathSegment
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.attachImage(name, mime, stream)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingReminder
        if (granted && pending != null) {
            viewModel.completePendingReminder(pending)
            pendingReminder = null
        } else if (!granted) {
            pendingReminder = null
        }
    }

    fun hasReminderPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) return false
        }
        return true
    }

    fun requestReminderPermissionsOrSchedule(dateId: Long, triggerAtMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                viewModel.scheduleReminderForConfirmedDate(
                    dateId = dateId,
                    triggerAtMs = triggerAtMs,
                    permissionsGranted = false,
                )
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
                return
            }
        }
        viewModel.scheduleReminderForConfirmedDate(
            dateId = dateId,
            triggerAtMs = triggerAtMs,
            permissionsGranted = true,
        )
    }

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen && !uiState.isLoading) onBack()
    }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OpportunityDetailEvent.OpenTask -> onOpenTask(event.taskId)
                is OpportunityDetailEvent.OpenExtraction ->
                    onOpenExtractionReview(event.opportunityId)
                is OpportunityDetailEvent.RequestReminderPermissions -> {
                    pendingReminder = event.pending
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    } else {
                        viewModel.completePendingReminder(event.pending)
                        pendingReminder = null
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opportunity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (opportunity != null && !opportunity.isDeleted) {
                        IconButton(onClick = { onEdit(opportunity.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::softDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
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
            opportunity == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Opportunity not found")
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
                    Text(opportunity.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        opportunity.statusLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (opportunity.status == OpportunityStatus.NEEDS_REVIEW) {
                            WarningAmber
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )

                    if (!opportunity.description.isNullOrBlank()) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            opportunity.description,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    DetailRow("Type", opportunity.type.name.lowercase().replaceFirstChar { it.titlecase() })
                    uiState.categoryName?.let { DetailRow("Category", it) }
                    if (opportunity.sourcePlatform != com.planwell.app.domain.model.SourcePlatform.UNKNOWN) {
                        DetailRow("Source", opportunity.sourcePlatform.label())
                    }
                    DetailRow("Saved", formatOpportunityDateTime(opportunity.createdAtMs))
                    DetailRow("Updated", formatOpportunityDateTime(opportunity.updatedAtMs))

                    val url = opportunity.sourceUrl
                    if (!url.isNullOrBlank()) {
                        Text("Source link", style = MaterialTheme.typography.titleMedium)
                        Text(url, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = {
                                val normalized = if (url.contains("://")) url else "https://$url"
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(normalized)),
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Text("  Open source")
                        }
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Text("Dates", style = MaterialTheme.typography.titleMedium)
                    if (opportunity.dates.isEmpty()) {
                        Text(
                            "No dates saved yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        opportunity.dates.forEach { date ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(date.typeLabel(), style = MaterialTheme.typography.titleSmall)
                                when {
                                    date.isUserConfirmed && date.dateMs != null -> {
                                        val urgency = deadlineUrgency(date.dateMs)
                                        Text(
                                            formatOpportunityDate(date.dateMs),
                                            color = when (urgency) {
                                                DeadlineUrgency.OVERDUE -> DangerRed
                                                DeadlineUrgency.UPCOMING -> WarningAmber
                                                DeadlineUrgency.NONE ->
                                                    MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                        TextButton(
                                            onClick = {
                                                pickDate(context, date.dateMs) { ms ->
                                                    viewModel.confirmDate(date.id, ms)
                                                }
                                            },
                                        ) {
                                            Text("Correct date")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                requestReminderPermissionsOrSchedule(
                                                    date.id,
                                                    date.dateMs,
                                                )
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Notifications,
                                                contentDescription = null,
                                            )
                                            Text("  Remind me")
                                        }
                                    }
                                    date.extractedDateMs != null -> {
                                        Text(
                                            "Suggested ${formatOpportunityDate(date.extractedDateMs)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            "Not confirmed",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WarningAmber,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.confirmDate(
                                                        date.id,
                                                        date.extractedDateMs,
                                                    )
                                                },
                                            ) {
                                                Text("Confirm suggestion")
                                            }
                                            TextButton(
                                                onClick = {
                                                    pickDate(context, date.extractedDateMs) { ms ->
                                                        viewModel.confirmDate(date.id, ms)
                                                    }
                                                },
                                            ) {
                                                Text("Pick another")
                                            }
                                        }
                                    }
                                    else -> {
                                        Text(
                                            "No date value",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        TextButton(
                                            onClick = {
                                                pickDate(context, null) { ms ->
                                                    viewModel.confirmDate(date.id, ms)
                                                }
                                            },
                                        ) {
                                            Text("Set date")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pickDate(context, null) { ms ->
                                viewModel.addDate(
                                    OpportunityDateType.APPLICATION_DEADLINE,
                                    ms,
                                    confirmed = true,
                                )
                            }
                        },
                    ) {
                        Text("Add application deadline")
                    }
                    OutlinedButton(
                        onClick = {
                            pickDate(context, null) { ms ->
                                viewModel.addDate(
                                    OpportunityDateType.EVENT,
                                    ms,
                                    confirmed = true,
                                )
                            }
                        },
                    ) {
                        Text("Add event date")
                    }

                    Text("Attachments", style = MaterialTheme.typography.titleMedium)
                    if (opportunity.attachments.isEmpty()) {
                        Text(
                            "No attachments",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        opportunity.attachments.forEach { attachment ->
                            Text(
                                attachment.displayName?.takeIf { it.isNotBlank() }
                                    ?: attachment.relativePath,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            attachment.mimeType?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (!opportunity.isDeleted) {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !uiState.attaching,
                        ) {
                            Text(if (uiState.attaching) "Attaching…" else "Add image")
                        }
                        val hasImage = opportunity.attachments.any {
                            it.kind == AttachmentKind.IMAGE ||
                                it.mimeType?.startsWith("image/") == true
                        }
                        if (hasImage) {
                            OutlinedButton(onClick = { onOpenExtractionReview(opportunity.id) }) {
                                Text("Review / re-run extraction")
                            }
                            Text(
                                "New images open extraction automatically. OCR stays on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (!opportunity.isDeleted) {
                        Text("Tasks & reminders", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { viewModel.createTask() },
                            enabled = !uiState.creatingTask,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (uiState.creatingTask) "Creating…" else "Create task",
                            )
                        }
                        Text(
                            "Uses confirmed deadline as due date when available. " +
                                "Unconfirmed OCR dates are never used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text("Linked tasks", style = MaterialTheme.typography.titleMedium)
                    if (uiState.linkedTasks.isEmpty()) {
                        Text(
                            "No linked tasks yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.linkedTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTask(task.id) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        buildString {
                                            append(
                                                when {
                                                    task.isDeleted -> "In trash"
                                                    task.isCompleted -> "Completed"
                                                    else -> "Active"
                                                },
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.unlinkTask(task.id) }) {
                                    Text("Unlink")
                                }
                            }
                        }
                    }

                    if (!opportunity.isDeleted && uiState.prepSuggestions.isNotEmpty()) {
                        Text(
                            "Suggested prep steps",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Optional ideas — not extracted from the source. " +
                                "Creates an undated linked task (duplicates are skipped).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        uiState.prepSuggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { viewModel.createPrepTask(suggestion) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.creatingTask,
                            ) {
                                Text("Add: $suggestion")
                            }
                        }
                    }

                    Text("Actions", style = MaterialTheme.typography.titleMedium)
                    if (opportunity.isDeleted) {
                        Button(onClick = viewModel::restore) { Text("Restore") }
                        OutlinedButton(onClick = viewModel::permanentlyDelete) {
                            Text("Delete forever")
                        }
                    } else {
                        when (opportunity.status) {
                            OpportunityStatus.ARCHIVED -> {
                                Button(onClick = viewModel::unarchive) { Text("Unarchive") }
                            }
                            else -> {
                                OutlinedButton(onClick = viewModel::archive) { Text("Archive") }
                            }
                        }
                        if (opportunity.status == OpportunityStatus.NEEDS_REVIEW) {
                            Button(
                                onClick = {
                                    viewModel.setStatus(OpportunityStatus.ACTIVE)
                                },
                            ) {
                                Text("Mark as reviewed")
                            }
                        }
                        if (opportunity.status != OpportunityStatus.COMPLETED) {
                            TextButton(
                                onClick = {
                                    viewModel.setStatus(OpportunityStatus.COMPLETED)
                                },
                            ) {
                                Text("Mark completed")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
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
                initialMs?.let { timeInMillis = it }
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
