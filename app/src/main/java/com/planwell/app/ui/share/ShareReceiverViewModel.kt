package com.planwell.app.ui.share

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.domain.importer.ImportStage
import com.planwell.app.domain.importer.SaveImportedEntryUseCase
import com.planwell.app.domain.importer.SmartImportPipeline
import com.planwell.app.domain.share.SharePayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareReceiverViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val pipeline: SmartImportPipeline,
    private val saveImportedEntry: SaveImportedEntryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareImportState>(
        ShareImportState.Loading("Preparing…"),
    )
    val state: StateFlow<ShareImportState> = _state.asStateFlow()

    private var payload: SharePayload? = restorePayload()
    private var pipelineJob: Job? = null

    init {
        payload?.let { startPipeline(it) }
    }

    fun setPayload(newPayload: SharePayload) {
        persist(newPayload)
        payload = newPayload
        startPipeline(newPayload)
    }

    fun setFatalError(message: String) {
        _state.value = ShareImportState.Error(message, retryable = false)
    }

    fun retry() {
        val p = payload ?: return
        startPipeline(p)
    }

    fun updateDraft(transform: (EditableImportDraft) -> EditableImportDraft) {
        val current = _state.value as? ShareImportState.Review ?: return
        _state.value = ShareImportState.Review(transform(current.draft))
    }

    fun setKind(kind: EntryKind) = updateDraft { it.copy(kind = kind) }
    fun setTitle(title: String) = updateDraft { it.copy(title = title) }
    fun setNotes(notes: String) = updateDraft { it.copy(notes = notes) }
    fun setDue(dueMs: Long?) = updateDraft {
        it.copy(dueOrStartMs = dueMs, timeWasExplicit = dueMs != null)
    }

    fun clearPhone() = updateDraft { it.copy(phone = null) }
    fun clearAddress() = updateDraft { it.copy(address = null) }
    fun clearUrl() = updateDraft { it.copy(url = null) }
    fun clearMoney() = updateDraft { it.copy(money = null) }

    fun save() {
        val review = _state.value as? ShareImportState.Review ?: return
        viewModelScope.launch {
            _state.value = ShareImportState.Saving
            runCatching {
                saveImportedEntry(review.draft.toImportedEntry())
            }.onSuccess { id ->
                _state.value = ShareImportState.Saved(id)
            }.onFailure { e ->
                _state.value = ShareImportState.Error(
                    message = e.message ?: "Could not save",
                    retryable = true,
                )
            }
        }
    }

    private fun startPipeline(p: SharePayload) {
        pipelineJob?.cancel()
        pipelineJob = viewModelScope.launch {
            _state.value = ShareImportState.Loading("Starting…")
            pipeline.process(p).collect { stage ->
                when (stage) {
                    is ImportStage.Progress ->
                        _state.value = ShareImportState.Loading(stage.label)
                    is ImportStage.Ready ->
                        _state.value = ShareImportState.Review(
                            EditableImportDraft.from(stage.entry),
                        )
                    is ImportStage.Failed ->
                        _state.value = ShareImportState.Error(stage.message, retryable = true)
                }
            }
        }
    }

    private fun persist(payload: SharePayload) {
        when (payload) {
            is SharePayload.Text -> {
                savedStateHandle[KEY_KIND] = KIND_TEXT
                savedStateHandle[KEY_TEXT] = payload.text
                savedStateHandle[KEY_SUBJECT] = payload.subject
                savedStateHandle.remove<ArrayList<String>>(KEY_URIS)
            }
            is SharePayload.Image -> {
                savedStateHandle[KEY_KIND] = KIND_IMAGE
                savedStateHandle[KEY_URIS] = arrayListOf(payload.uri.toString())
                savedStateHandle.remove<String>(KEY_TEXT)
                savedStateHandle.remove<String>(KEY_SUBJECT)
            }
            is SharePayload.MultiImage -> {
                savedStateHandle[KEY_KIND] = KIND_MULTI
                savedStateHandle[KEY_URIS] = ArrayList(payload.uris.map { it.toString() })
                savedStateHandle.remove<String>(KEY_TEXT)
                savedStateHandle.remove<String>(KEY_SUBJECT)
            }
        }
    }

    private fun restorePayload(): SharePayload? {
        val kind = savedStateHandle.get<String>(KEY_KIND) ?: return null
        return when (kind) {
            KIND_TEXT -> SharePayload.Text(
                text = savedStateHandle.get<String>(KEY_TEXT).orEmpty(),
                subject = savedStateHandle.get<String>(KEY_SUBJECT),
            )
            KIND_IMAGE -> {
                val uri = savedStateHandle.get<ArrayList<String>>(KEY_URIS)?.firstOrNull()
                    ?: return null
                SharePayload.Image(Uri.parse(uri))
            }
            KIND_MULTI -> {
                val uris = savedStateHandle.get<ArrayList<String>>(KEY_URIS).orEmpty()
                    .map(Uri::parse)
                if (uris.isEmpty()) null else SharePayload.MultiImage(uris)
            }
            else -> null
        }
    }

    companion object {
        private const val KEY_KIND = "share_kind"
        private const val KEY_TEXT = "share_text"
        private const val KEY_SUBJECT = "share_subject"
        private const val KEY_URIS = "share_uris"
        private const val KIND_TEXT = "text"
        private const val KIND_IMAGE = "image"
        private const val KIND_MULTI = "multi"
    }
}
