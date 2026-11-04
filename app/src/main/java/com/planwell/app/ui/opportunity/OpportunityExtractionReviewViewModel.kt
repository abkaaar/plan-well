package com.planwell.app.ui.opportunity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityCandidate
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.usecase.opportunity.ApplyExtractionReviewUseCase
import com.planwell.app.domain.usecase.opportunity.DateReviewDecision
import com.planwell.app.domain.usecase.opportunity.ExtractionReviewDecision
import com.planwell.app.domain.usecase.opportunity.ObserveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.RunOpportunityExtractionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CandidateReviewRow(
    val candidate: OpportunityCandidate,
    val editedValue: String,
    val accepted: Boolean? = null,
)

data class DateReviewRow(
    val date: OpportunityDate,
    val confirmedDateMs: Long?,
    val accepted: Boolean? = null,
)

data class ExtractionReviewUiState(
    val opportunity: Opportunity? = null,
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val candidateRows: List<CandidateReviewRow> = emptyList(),
    val dateRows: List<DateReviewRow> = emptyList(),
    val rawTextExpanded: Boolean = false,
    val done: Boolean = false,
    val openDetailForAct: Boolean = false,
)

private data class ReviewEdits(
    val candidateEdits: Map<Long, String> = emptyMap(),
    val candidateAccept: Map<Long, Boolean> = emptyMap(),
    val dateConfirmMs: Map<Long, Long?> = emptyMap(),
    val dateAccept: Map<Long, Boolean> = emptyMap(),
    val isRunning: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    val openDetailForAct: Boolean = false,
    val rawExpanded: Boolean = false,
)

@HiltViewModel
class OpportunityExtractionReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeOpportunity: ObserveOpportunityUseCase,
    private val runExtraction: RunOpportunityExtractionUseCase,
    private val applyReview: ApplyExtractionReviewUseCase,
) : ViewModel() {

    val opportunityId: Long = checkNotNull(savedStateHandle["opportunityId"])
    private val edits = MutableStateFlow(ReviewEdits())

    val uiState: StateFlow<ExtractionReviewUiState> =
        combine(observeOpportunity(opportunityId), edits) { opportunity, review ->
            val pendingCandidates = opportunity?.candidates
                ?.filter { !it.isApplied && !it.isRejected }
                .orEmpty()
            val pendingDates = opportunity?.dates
                ?.filter { !it.isUserConfirmed }
                .orEmpty()
            ExtractionReviewUiState(
                opportunity = opportunity,
                isLoading = opportunity == null,
                isRunning = review.isRunning,
                isSaving = review.isSaving,
                error = review.error
                    ?: opportunity?.extractionError?.takeIf {
                        opportunity.extractionStatus == ExtractionStatus.FAILED
                    },
                candidateRows = pendingCandidates.map { c ->
                    CandidateReviewRow(
                        candidate = c,
                        editedValue = review.candidateEdits[c.id] ?: c.valueText,
                        accepted = review.candidateAccept[c.id],
                    )
                },
                dateRows = pendingDates.map { d ->
                    DateReviewRow(
                        date = d,
                        confirmedDateMs = review.dateConfirmMs[d.id] ?: d.extractedDateMs,
                        accepted = review.dateAccept[d.id],
                    )
                },
                rawTextExpanded = review.rawExpanded,
                done = review.done,
                openDetailForAct = review.openDetailForAct,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExtractionReviewUiState(),
        )

    init {
        runExtractionIfNeeded()
    }

    fun runExtractionIfNeeded(force: Boolean = false) {
        viewModelScope.launch {
            edits.update {
                it.copy(
                    isRunning = true,
                    error = null,
                    candidateAccept = if (force) emptyMap() else it.candidateAccept,
                    dateAccept = if (force) emptyMap() else it.dateAccept,
                )
            }
            val result = runCatching { runExtraction(opportunityId) }.getOrElse { e ->
                edits.update {
                    it.copy(isRunning = false, error = e.message ?: "Extraction failed")
                }
                return@launch
            }
            edits.update {
                it.copy(
                    isRunning = false,
                    error = result.errorMessage.takeIf {
                        result.status == ExtractionStatus.FAILED ||
                            result.status == ExtractionStatus.SKIPPED
                    },
                )
            }
        }
    }

    fun toggleRawText() = edits.update { it.copy(rawExpanded = !it.rawExpanded) }

    fun onCandidateEdit(id: Long, value: String) {
        edits.update { it.copy(candidateEdits = it.candidateEdits + (id to value)) }
    }

    fun onCandidateAccept(id: Long, accept: Boolean) {
        edits.update { it.copy(candidateAccept = it.candidateAccept + (id to accept)) }
    }

    fun onDateAccept(id: Long, accept: Boolean) {
        edits.update { it.copy(dateAccept = it.dateAccept + (id to accept)) }
    }

    fun onDatePicked(id: Long, ms: Long) {
        edits.update {
            it.copy(
                dateConfirmMs = it.dateConfirmMs + (id to ms),
                dateAccept = it.dateAccept + (id to true),
            )
        }
    }

    fun applyAndFinish(openDetailForAct: Boolean = true) {
        viewModelScope.launch {
            val snapshot = edits.value
            edits.update { it.copy(isSaving = true, error = null) }
            runCatching {
                applyReview(
                    opportunityId = opportunityId,
                    textDecisions = snapshot.candidateAccept.map { (id, accept) ->
                        ExtractionReviewDecision(
                            candidateId = id,
                            accept = accept,
                            editedValue = snapshot.candidateEdits[id],
                        )
                    },
                    dateDecisions = snapshot.dateAccept.map { (id, accept) ->
                        DateReviewDecision(
                            dateId = id,
                            accept = accept,
                            confirmedDateMs = snapshot.dateConfirmMs[id],
                        )
                    },
                )
            }.onSuccess {
                edits.update {
                    it.copy(
                        isSaving = false,
                        done = true,
                        openDetailForAct = openDetailForAct,
                    )
                }
            }.onFailure { e ->
                edits.update {
                    it.copy(isSaving = false, error = e.message ?: "Could not apply review")
                }
            }
        }
    }

    fun skipToManual() {
        edits.update { it.copy(done = true, openDetailForAct = true) }
    }
}
