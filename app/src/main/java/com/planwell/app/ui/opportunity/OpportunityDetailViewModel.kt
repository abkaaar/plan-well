package com.planwell.app.ui.opportunity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.opportunity.ArchiveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.AttachOpportunityImageUseCase
import com.planwell.app.domain.usecase.opportunity.ConfirmOpportunityDateUseCase
import com.planwell.app.domain.usecase.opportunity.CreateTaskFromOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.OpportunityPrepSuggestions
import com.planwell.app.domain.usecase.opportunity.PermanentlyDeleteOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.RestoreOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.ScheduleConfirmedDeadlineReminderUseCase
import com.planwell.app.domain.usecase.opportunity.SetOpportunityStatusUseCase
import com.planwell.app.domain.usecase.opportunity.SoftDeleteOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.UnarchiveOpportunityUseCase
import com.planwell.app.domain.usecase.opportunity.UnlinkOpportunityTaskUseCase
import com.planwell.app.domain.usecase.opportunity.UpsertOpportunityDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

data class OpportunityDetailUiState(
    val opportunity: Opportunity? = null,
    val categoryName: String? = null,
    val categories: List<Category> = emptyList(),
    val linkedTasks: List<Task> = emptyList(),
    val prepSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val closeScreen: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val attaching: Boolean = false,
    val creatingTask: Boolean = false,
)

sealed interface OpportunityDetailEvent {
    data class OpenTask(val taskId: Long) : OpportunityDetailEvent
    data class OpenExtraction(val opportunityId: Long) : OpportunityDetailEvent
    data class RequestReminderPermissions(val pending: PendingReminder) : OpportunityDetailEvent
}

data class PendingReminder(
    val taskId: Long,
    val dateId: Long,
    val triggerAtMs: Long,
)

private data class DetailFlags(
    val closeScreen: Boolean = false,
    val attaching: Boolean = false,
    val creatingTask: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OpportunityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeOpportunity: ObserveOpportunityUseCase,
    getCategories: GetCategoriesUseCase,
    private val taskRepository: TaskRepository,
    private val softDeleteUseCase: SoftDeleteOpportunityUseCase,
    private val restoreUseCase: RestoreOpportunityUseCase,
    private val archiveUseCase: ArchiveOpportunityUseCase,
    private val unarchiveUseCase: UnarchiveOpportunityUseCase,
    private val permanentlyDeleteUseCase: PermanentlyDeleteOpportunityUseCase,
    private val setStatusUseCase: SetOpportunityStatusUseCase,
    private val confirmDateUseCase: ConfirmOpportunityDateUseCase,
    private val upsertDateUseCase: UpsertOpportunityDateUseCase,
    private val attachImageUseCase: AttachOpportunityImageUseCase,
    private val createTaskFromOpportunity: CreateTaskFromOpportunityUseCase,
    private val scheduleConfirmedReminder: ScheduleConfirmedDeadlineReminderUseCase,
    private val unlinkTaskUseCase: UnlinkOpportunityTaskUseCase,
) : ViewModel() {

    private val opportunityId: Long = checkNotNull(savedStateHandle["opportunityId"])
    private val flags = MutableStateFlow(DetailFlags())

    private val _events = MutableSharedFlow<OpportunityDetailEvent>()
    val events: SharedFlow<OpportunityDetailEvent> = _events.asSharedFlow()

    private val opportunityFlow = observeOpportunity(opportunityId)
    private val linkedTasksFlow = opportunityFlow.flatMapLatest { opp ->
        val ids = opp?.linkedTaskIds.orEmpty()
        if (ids.isEmpty()) flowOf(emptyList()) else taskRepository.observeTasksByIds(ids)
    }

    val uiState: StateFlow<OpportunityDetailUiState> =
        combine(
            opportunityFlow,
            getCategories(),
            linkedTasksFlow,
            flags,
        ) { opportunity, categories, linkedTasks, flagState ->
            OpportunityDetailUiState(
                opportunity = opportunity,
                categoryName = opportunity?.categoryId?.let { id ->
                    categories.firstOrNull { it.id == id }?.name
                },
                categories = categories,
                linkedTasks = linkedTasks,
                prepSuggestions = opportunity?.let { OpportunityPrepSuggestions.forOpportunity(it) }
                    .orEmpty(),
                isLoading = false,
                closeScreen = flagState.closeScreen || opportunity == null,
                error = flagState.error,
                message = flagState.message,
                attaching = flagState.attaching,
                creatingTask = flagState.creatingTask,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OpportunityDetailUiState(),
        )

    fun attachImage(displayName: String?, mimeType: String?, input: InputStream) {
        viewModelScope.launch {
            flags.update { it.copy(attaching = true, error = null) }
            runCatching {
                attachImageUseCase(opportunityId, displayName, mimeType, input)
            }.onSuccess {
                flags.update { it.copy(attaching = false, message = "Image attached — opening extraction") }
                _events.emit(OpportunityDetailEvent.OpenExtraction(opportunityId))
            }.onFailure {
                flags.update { f ->
                    f.copy(
                        attaching = false,
                        error = it.message ?: "Could not attach image",
                    )
                }
            }
        }
    }

    fun createTask(
        titleOverride: String? = null,
        useConfirmedDeadlineAsDue: Boolean = true,
    ) {
        viewModelScope.launch {
            flags.update { it.copy(creatingTask = true, error = null) }
            runCatching {
                createTaskFromOpportunity(
                    opportunityId = opportunityId,
                    title = titleOverride,
                    useConfirmedDeadlineAsDue = useConfirmedDeadlineAsDue,
                )
            }.onSuccess { result ->
                flags.update {
                    it.copy(creatingTask = false, message = "Task created")
                }
                _events.emit(OpportunityDetailEvent.OpenTask(result.taskId))
            }.onFailure {
                flags.update {
                    it.copy(
                        creatingTask = false,
                        error = it.message ?: "Could not create task",
                    )
                }
            }
        }
    }

    /** Prep steps are optional suggestions — never auto-dated; skip duplicates. */
    fun createPrepTask(suggestion: String) {
        val linked = uiState.value.linkedTasks
        if (linked.any { !it.isDeleted && it.title.equals(suggestion, ignoreCase = true) }) {
            viewModelScope.launch {
                flags.update { it.copy(message = "That prep task is already linked") }
            }
            return
        }
        createTask(titleOverride = suggestion, useConfirmedDeadlineAsDue = false)
    }

    fun scheduleReminderForConfirmedDate(
        dateId: Long,
        triggerAtMs: Long? = null,
        permissionsGranted: Boolean = true,
    ) {
        viewModelScope.launch {
            flags.update { it.copy(error = null) }
            val opportunity = uiState.value.opportunity ?: return@launch
            val date = opportunity.dates.firstOrNull { it.id == dateId } ?: return@launch
            if (!date.isUserConfirmed || date.dateMs == null) {
                flags.update {
                    it.copy(error = "Confirm the date before scheduling a reminder")
                }
                return@launch
            }
            val trigger = triggerAtMs ?: date.dateMs
            var taskId = uiState.value.linkedTasks.firstOrNull { !it.isDeleted }?.id
            if (taskId == null) {
                val created = runCatching {
                    createTaskFromOpportunity(
                        opportunityId = opportunityId,
                        useConfirmedDeadlineAsDue = true,
                    )
                }.getOrElse { e ->
                    flags.update { it.copy(error = e.message) }
                    return@launch
                }
                taskId = created.taskId
            }
            if (!permissionsGranted) {
                _events.emit(
                    OpportunityDetailEvent.RequestReminderPermissions(
                        PendingReminder(taskId, dateId, trigger),
                    ),
                )
                return@launch
            }
            runCatching {
                scheduleConfirmedReminder(
                    opportunityId = opportunityId,
                    taskId = taskId,
                    opportunityDateId = dateId,
                    triggerAtMs = trigger,
                    repeatType = RepeatType.NONE,
                )
            }.onSuccess {
                flags.update { it.copy(message = "Reminder scheduled") }
            }.onFailure { e ->
                flags.update {
                    it.copy(error = e.message ?: "Could not schedule reminder")
                }
            }
        }
    }

    fun completePendingReminder(pending: PendingReminder) {
        viewModelScope.launch {
            runCatching {
                scheduleConfirmedReminder(
                    opportunityId = opportunityId,
                    taskId = pending.taskId,
                    opportunityDateId = pending.dateId,
                    triggerAtMs = pending.triggerAtMs,
                )
            }.onSuccess {
                flags.update { it.copy(message = "Reminder scheduled") }
            }.onFailure { e ->
                flags.update {
                    it.copy(error = e.message ?: "Could not schedule reminder")
                }
            }
        }
    }

    fun unlinkTask(taskId: Long) {
        viewModelScope.launch {
            unlinkTaskUseCase(opportunityId, taskId)
            flags.update { it.copy(message = "Task unlinked (task kept)") }
        }
    }

    fun softDelete() {
        viewModelScope.launch {
            softDeleteUseCase(opportunityId)
            flags.update { it.copy(closeScreen = true) }
        }
    }

    fun restore() {
        viewModelScope.launch { restoreUseCase(opportunityId) }
    }

    fun archive() {
        viewModelScope.launch { archiveUseCase(opportunityId) }
    }

    fun unarchive() {
        viewModelScope.launch { unarchiveUseCase(opportunityId) }
    }

    fun permanentlyDelete() {
        viewModelScope.launch {
            permanentlyDeleteUseCase(opportunityId)
            flags.update { it.copy(closeScreen = true) }
        }
    }

    fun setStatus(status: OpportunityStatus) {
        viewModelScope.launch { setStatusUseCase(opportunityId, status) }
    }

    fun confirmDate(dateId: Long, confirmedDateMs: Long) {
        viewModelScope.launch { confirmDateUseCase(dateId, confirmedDateMs) }
    }

    fun addDate(type: OpportunityDateType, dateMs: Long, confirmed: Boolean) {
        viewModelScope.launch {
            upsertDateUseCase(
                OpportunityDate(
                    opportunityId = opportunityId,
                    type = type,
                    dateMs = if (confirmed) dateMs else null,
                    extractedDateMs = if (!confirmed) dateMs else null,
                    isUserConfirmed = confirmed,
                ),
            )
        }
    }

    fun clearMessage() {
        flags.update { it.copy(message = null) }
    }
}
