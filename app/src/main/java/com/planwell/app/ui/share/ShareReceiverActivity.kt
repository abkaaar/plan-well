package com.planwell.app.ui.share

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planwell.app.domain.share.toSharePayload
import com.planwell.app.ui.theme.PlanWellTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlanWellTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                LaunchedEffect(Unit) {
                    if (viewModel.state.value is ShareImportState.Review ||
                        viewModel.state.value is ShareImportState.Loading &&
                        (viewModel.state.value as? ShareImportState.Loading)?.stage != "Preparing…"
                    ) {
                        // Pipeline may already be running from SavedState restore
                    }
                    // Only parse intent if we don't already have a restored/running import
                    val current = viewModel.state.value
                    val alreadyGoing = current is ShareImportState.Review ||
                        current is ShareImportState.Saving ||
                        current is ShareImportState.Saved ||
                        (current is ShareImportState.Loading && current.stage != "Preparing…")
                    if (alreadyGoing) return@LaunchedEffect

                    val raw = intent?.toSharePayload()
                    if (raw == null) {
                        viewModel.setFatalError("Nothing to save")
                        return@LaunchedEffect
                    }
                    val retained = runCatching {
                        withContext(Dispatchers.IO) {
                            ShareIncomingUriRetainer(this@ShareReceiverActivity)
                                .retain(raw, intent?.flags ?: 0)
                        }
                    }.getOrElse { e ->
                        viewModel.setFatalError(e.message ?: "Could not read shared content")
                        return@LaunchedEffect
                    }
                    viewModel.setPayload(retained)
                }

                LaunchedEffect(state) {
                    when (val s = state) {
                        is ShareImportState.Saved -> {
                            Toast.makeText(
                                this@ShareReceiverActivity,
                                "Saved to Plan Well",
                                Toast.LENGTH_SHORT,
                            ).show()
                            finish()
                        }
                        is ShareImportState.Error -> {
                            if (!s.retryable) {
                                Toast.makeText(
                                    this@ShareReceiverActivity,
                                    s.message,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                finish()
                            }
                        }
                        else -> Unit
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                ) {
                    ShareImportScreen(
                        state = state,
                        onCancel = { finish() },
                        onRetry = viewModel::retry,
                        onSave = viewModel::save,
                        onKindChange = viewModel::setKind,
                        onTitleChange = viewModel::setTitle,
                        onNotesChange = viewModel::setNotes,
                        onDueChange = viewModel::setDue,
                        onClearPhone = viewModel::clearPhone,
                        onClearAddress = viewModel::clearAddress,
                        onClearUrl = viewModel::clearUrl,
                        onClearMoney = viewModel::clearMoney,
                    )
                }
            }
        }
    }
}
