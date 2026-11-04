package com.planwell.app.ui.opportunity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.CaptureMethod
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.SourcePlatform
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.opportunity.CreateOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.GetOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.SourcePlatformInference
import com.planwell.app.domain.usecase.opportunity.UpdateOpportunityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditOpportunityUiState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val type: OpportunityType = OpportunityType.OPPORTUNITY,
    val categoryId: Long? = null,
    val status: OpportunityStatus = OpportunityStatus.NEEDS_REVIEW,
    val sourceUrl: String = "",
    val sourcePlatform: SourcePlatform = SourcePlatform.UNKNOWN,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val savedId: Long? = null,
)

@HiltViewModel
class AddEditOpportunityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCategories: GetCategoriesUseCase,
    private val getOpportunity: GetOpportunityUseCase,
    private val createOpportunity: CreateOpportunityUseCase,
    private val updateOpportunity: UpdateOpportunityUseCase,
) : ViewModel() {

    private val opportunityId: Long? =
        savedStateHandle.get<Long>("opportunityId")?.takeIf { it > 0 }

    private val form = MutableStateFlow(
        AddEditOpportunityUiState(isEditing = opportunityId != null),
    )

    val uiState: StateFlow<AddEditOpportunityUiState> =
        combine(form, getCategories()) { state, categories ->
            state.copy(categories = categories)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AddEditOpportunityUiState(isEditing = opportunityId != null),
        )

    init {
        val id = opportunityId
        if (id != null) {
            viewModelScope.launch {
                val existing = getOpportunity(id)
                if (existing != null) {
                    form.update {
                        it.copy(
                            title = existing.title,
                            description = existing.description.orEmpty(),
                            type = existing.type,
                            categoryId = existing.categoryId,
                            status = existing.status,
                            sourceUrl = existing.sourceUrl.orEmpty(),
                            sourcePlatform = existing.sourcePlatform,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = form.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = form.update { it.copy(description = value) }
    fun onTypeChange(value: OpportunityType) = form.update { it.copy(type = value) }
    fun onCategoryChange(value: Long?) = form.update { it.copy(categoryId = value) }
    fun onStatusChange(value: OpportunityStatus) = form.update { it.copy(status = value) }
    fun onSourceUrlChange(value: String) = form.update {
        val inferred = SourcePlatformInference.fromUrl(value.trim().ifBlank { null })
        it.copy(
            sourceUrl = value,
            sourcePlatform = if (inferred != SourcePlatform.UNKNOWN) {
                inferred
            } else {
                it.sourcePlatform
            },
        )
    }

    fun onSourcePlatformChange(value: SourcePlatform) =
        form.update { it.copy(sourcePlatform = value) }

    fun save() {
        viewModelScope.launch {
            val state = form.value
            runCatching {
                val draft = Opportunity(
                    id = opportunityId ?: 0L,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    type = state.type,
                    categoryId = state.categoryId,
                    status = state.status,
                    sourceUrl = state.sourceUrl.ifBlank { null },
                    sourcePlatform = state.sourcePlatform,
                    captureMethod = if (opportunityId == null) {
                        CaptureMethod.MANUAL
                    } else {
                        CaptureMethod.UNKNOWN
                    },
                )
                if (opportunityId == null) {
                    val id = createOpportunity(draft.copy(captureMethod = CaptureMethod.MANUAL))
                    form.update { it.copy(saved = true, savedId = id) }
                } else {
                    val existing = getOpportunity(opportunityId) ?: return@launch
                    updateOpportunity(
                        existing.copy(
                            title = draft.title,
                            description = draft.description,
                            type = draft.type,
                            categoryId = draft.categoryId,
                            status = draft.status,
                            sourceUrl = draft.sourceUrl,
                            sourcePlatform = draft.sourcePlatform,
                        ),
                    )
                    form.update { it.copy(saved = true, savedId = opportunityId) }
                }
            }.onFailure { e ->
                form.update { it.copy(error = e.message ?: "Could not save") }
            }
        }
    }
}
