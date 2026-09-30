package com.planwell.app.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.planwell.app.domain.model.RepeatType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerSheet(
    onDismiss: () -> Unit,
    onConfirm: (triggerAtMs: Long, repeatType: RepeatType) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    var triggerAtMs by remember {
        mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1000L)
    }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Set reminder", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMs }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val next = Calendar.getInstance().apply {
                                timeInMillis = triggerAtMs
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
                                    triggerAtMs = next.timeInMillis
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
                Text(dateFormat.format(Date(triggerAtMs)))
            }
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = repeatType == type,
                        onClick = { repeatType = type },
                        label = {
                            Text(
                                when (type) {
                                    RepeatType.NONE -> "Once"
                                    RepeatType.DAILY -> "Daily"
                                    RepeatType.WEEKLY -> "Weekly"
                                },
                            )
                        },
                    )
                }
            }
            Button(
                onClick = { onConfirm(triggerAtMs, repeatType) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save reminder")
            }
        }
    }
}
