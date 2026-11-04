package com.planwell.app.ui.opportunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.opportunity.ArchiveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.CaptureSharedContentUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveArchivedOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveDeletedOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveInboxOpportunitiesUseCase
import com.planwell.app.domain.usecase.opportunity.PermanentlyDeleteOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.RestoreOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.SoftDeleteOpportunityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpportunityInboxUiState(
    val items: List<Opportunity> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryNames: Map<Long, String> = emptyMap(),
    val query: String = "",
    val statusFilter: OpportunityListFilter = OpportunityListFilter.INBOX,
    val categoryId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface OpportunityInboxEvent {
    data class ShowUndoDelete(val id: Long, val title: String) : OpportunityInboxEvent
    data class ShowMessage(val message: String) : OpportunityInboxEvent
}

private data class InboxFilters(
    val query: String = "",
    val statusFilter: OpportunityListFilter = OpportunityListFilter.INBOX,
    val categoryId: Long? = null,
)

@HiltViewModel
class OpportunityInboxViewModel @Inject constructor(
    observeInbox: ObserveInboxOpportunitiesUseCase,
    observeArchived: ObserveArchivedOpportunitiesUseCase,
    observeDeleted: ObserveDeletedOpportunitiesUseCase,
    getCategories: GetCategoriesUseCase,
    val captureSharedContent: CaptureSharedContentUseCase,
    private val softDeleteUseCase: SoftDeleteOpportunityUseCase,
    private val restoreUseCase: RestoreOpportunityUseCase,
    private val archiveUseCase: ArchiveOpportunityUseCase,
    private val permanentlyDeleteUseCase: PermanentlyDeleteOpportunityUseCase,
) : ViewModel() {

    private val filters = MutableStateFlow(InboxFilters())

    private val _events = MutableSharedFlow<OpportunityInboxEvent>()
    val events: SharedFlow<OpportunityInboxEvent> = _events.asSharedFlow()

    val uiState: StateFlow<OpportunityInboxUiState> =
        combine(
            observeInbox(),
            observeArchived(),
            observeDeleted(),
            getCategories(),
            filters,
        ) { inbox, archived, deleted, categories, filterState ->
            val base = when (filterState.statusFilter) {
                OpportunityListFilter.INBOX -> inbox
                OpportunityListFilter.NEEDS_REVIEW ->
                    inbox.filter { it.status == OpportunityStatus.NEEDS_REVIEW }
                OpportunityListFilter.ACTIVE ->
                    inbox.filter { it.status == OpportunityStatus.ACTIVE }
                OpportunityListFilter.COMPLETED ->
                    inbox.filter { it.status == OpportunityStatus.COMPLETED }
                OpportunityListFilter.ARCHIVED -> archived
                OpportunityListFilter.TRASH -> deleted
            }
            val filtered = base
                .filter {
                    filterState.categoryId == null || it.categoryId == filterState.categoryId
                }
                .filter { it.matchesQuery(filterState.query) }

            OpportunityInboxUiState(
                items = filtered,
                categories = categories,
                categoryNames = categories.associate { it.id to it.name },
                query = filterState.query,
                statusFilter = filterState.statusFilter,
                categoryId = filterState.categoryId,
                isLoading = false,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OpportunityInboxUiState(),
        )

    fun onQueryChange(value: String) {
        filters.update { it.copy(query = value) }
    }

    fun onStatusFilterChange(filter: OpportunityListFilter) {
        filters.update { it.copy(statusFilter = filter) }
    }

    fun onCategoryFilterChange(id: Long?) {
        filters.update { it.copy(categoryId = id) }
    }

    fun delete(opportunity: Opportunity) {
        viewModelScope.launch {
            softDeleteUseCase(opportunity.id)
            _events.emit(OpportunityInboxEvent.ShowUndoDelete(opportunity.id, opportunity.title))
        }
    }

    fun undoDelete(id: Long) {
        viewModelScope.launch { restoreUseCase(id) }
    }

    fun restore(id: Long) {
        viewModelScope.launch { restoreUseCase(id) }
    }

    fun archive(opportunity: Opportunity) {
        viewModelScope.launch {
            archiveUseCase(opportunity.id)
            _events.emit(OpportunityInboxEvent.ShowMessage("Archived “${opportunity.title}”"))
        }
    }

    fun permanentlyDelete(id: Long) {
        viewModelScope.launch { permanentlyDeleteUseCase(id) }
    }
}
