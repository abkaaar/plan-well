package com.planwell.app.ui.opportunity

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.planwell.app.domain.usecase.opportunity.CaptureSharedContentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Capture entry: paste, pick image, or fall back to manual form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureOpportunitySheet(
    captureSharedContent: CaptureSharedContentUseCase,
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onCaptured: (opportunityId: Long, openExtraction: Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPaste by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri)
                    val name = uri.lastPathSegment
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        captureSharedContent.fromImage(
                            displayName = name,
                            mimeType = mime,
                            input = stream,
                            captureMethod = com.planwell.app.domain.model.CaptureMethod.MANUAL,
                        )
                    } ?: error("Could not read image")
                }
            }
            busy = false
            result.onSuccess { capture ->
                onCaptured(capture.opportunityId, capture.hasImage)
                onDismiss()
            }.onFailure { e ->
                onError(e.message ?: "Could not capture image")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Save", style = MaterialTheme.typography.titleLarge)
            Text(
                "Share from other apps, or:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showPaste) {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    label = { Text("Text or URL") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    captureSharedContent.fromPaste(pasteText)
                                }
                            }
                            busy = false
                            result.onSuccess { capture ->
                                onCaptured(capture.opportunityId, false)
                                onDismiss()
                            }.onFailure { e ->
                                onError(e.message ?: "Could not save paste")
                            }
                        }
                    },
                    enabled = !busy && pasteText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (busy) "Saving…" else "Save")
                }
                TextButton(onClick = { showPaste = false }) { Text("Back") }
            } else {
                OutlinedButton(
                    onClick = { showPaste = true },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Paste text or URL" },
                ) {
                    Text("Paste")
                }
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Pick an image" },
                ) {
                    Text(if (busy) "Working…" else "Image")
                }
                OutlinedButton(
                    onClick = {
                        onManual()
                        onDismiss()
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Enter manually" },
                ) {
                    Text("Manual")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
