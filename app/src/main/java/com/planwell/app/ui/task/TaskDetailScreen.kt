package com.planwell.app.ui.task

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.ui.components.PriorityChip
import com.planwell.app.ui.reminder.ReminderPickerSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }
    val context = LocalContext.current
    var showReminderSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showReminderSheet = true
    }

    fun ensureReminderPermissionsThenOpenSheet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        showReminderSheet = true
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    val task = uiState.task

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (task != null) {
                        IconButton(onClick = { onEdit(task.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (task == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                Text(if (uiState.isLoading) "Loading…" else "Task not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(task.title, style = MaterialTheme.typography.headlineSmall)
                if (!task.description.isNullOrBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityChip(priority = task.priority)
                    Text(
                        text = if (task.isCompleted) "Completed" else "Active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                task.dueDateMs?.let {
                    Text(
                        "Due ${dateFormat.format(Date(it))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                uiState.categoryName?.let { name ->
                    Text(
                        "Category: $name",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Text("Reminders", style = MaterialTheme.typography.titleMedium)
                if (uiState.reminders.isEmpty()) {
                    Text(
                        "No reminders yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    uiState.reminders.forEach { reminder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateFormat.format(Date(reminder.triggerAtMs)))
                                Text(
                                    reminder.repeatType.name.lowercase().replaceFirstChar { it.titlecase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.cancelReminder(reminder.id) }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
                uiState.reminderError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                OutlinedButton(
                    onClick = ::ensureReminderPermissionsThenOpenSheet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Text("  Add reminder")
                }
                Button(
                    onClick = viewModel::toggleComplete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (task.isCompleted) "Mark incomplete" else "Mark complete")
                }
                OutlinedButton(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Move to trash")
                }
            }
        }
    }

    if (showReminderSheet) {
        ReminderPickerSheet(
            onDismiss = { showReminderSheet = false },
            onConfirm = { triggerAtMs, repeatType ->
                viewModel.scheduleReminder(triggerAtMs, repeatType)
                showReminderSheet = false
            },
        )
    }
}
