package com.planwell.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.agenda.AgendaItem
import com.planwell.app.domain.agenda.AgendaSuggestion
import com.planwell.app.domain.agenda.DailyAgenda
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.agenda.ObserveDailyAgendaUseCase
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.opportunity.CaptureSharedContentUseCase
import com.planwell.app.domain.usecase.opportunity.ObserveInboxOpportunitiesUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.CompletionStatsUseCase
import com.planwell.app.domain.usecase.task.DeleteTaskUseCase
import com.planwell.app.domain.usecase.task.ExportTasksUseCase
import com.planwell.app.domain.usecase.task.GetTasksUseCase
import com.planwell.app.domain.usecase.task.RestoreTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val undatedTasks: List<Task> = emptyList(),
    val agenda: DailyAgenda? = null,
    val agendaItems: List<AgendaItem> = emptyList(),
    val suggestions: List<AgendaSuggestion> = emptyList(),
    val needsReview: List<Opportunity> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val stats: CompletionStats = CompletionStats(0, 0),
    val categoryNames: Map<Long?, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface TaskListEvent {
    data class ShowUndoDelete(val taskId: Long, val title: String) : TaskListEvent
    data class ShowMessage(val message: String) : TaskListEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getTasksUseCase: GetTasksUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    completionStatsUseCase: CompletionStatsUseCase,
    observeDailyAgenda: ObserveDailyAgendaUseCase,
    observeInbox: ObserveInboxOpportunitiesUseCase,
    val captureSharedContent: CaptureSharedContentUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val exportTasksUseCase: ExportTasksUseCase,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    private val _events = MutableSharedFlow<TaskListEvent>()
    val events: SharedFlow<TaskListEvent> = _events.asSharedFlow()

    private data class HomeSources(
        val tasks: List<Task>,
        val categories: List<Category>,
        val stats: CompletionStats,
        val agenda: DailyAgenda,
        val opportunities: List<Opportunity>,
    )

    val uiState: StateFlow<TaskListUiState> =
        combine(
            combine(
                getTasksUseCase(),
                getCategoriesUseCase(),
                completionStatsUseCase(),
                observeDailyAgenda(),
                observeInbox(),
            ) { tasks, categories, stats, agenda, opportunities ->
                HomeSources(tasks, categories, stats, agenda, opportunities)
            },
            selectedCategoryId,
        ) { sources, selectedId ->
            val tasks = sources.tasks
            val categories = sources.categories
            val stats = sources.stats
            val agenda = sources.agenda
            val opportunities = sources.opportunities

            val filtered = if (selectedId == null) {
                tasks
            } else {
                tasks.filter { it.categoryId == selectedId }
            }
            val undated = filtered.filter { !it.isCompleted && it.dueDateMs == null }
            val datedOrDone = filtered.filter { it.dueDateMs != null || it.isCompleted }

            val agendaItems = if (selectedId == null) {
                agenda.items
            } else {
                agenda.items.filter { item ->
                    when {
                        item.taskId != null ->
                            tasks.any { it.id == item.taskId && it.categoryId == selectedId }
                        item.opportunityId != null -> {
                            val opp = opportunities.firstOrNull { it.id == item.opportunityId }
                            opp?.categoryId == selectedId ||
                                opp?.linkedTaskIds?.any { tid ->
                                    tasks.any { it.id == tid && it.categoryId == selectedId }
                                } == true
                        }
                        else -> false
                    }
                }
            }

            val needsReview = opportunities
                .filter { it.status == OpportunityStatus.NEEDS_REVIEW && !it.isDeleted }
                .let { list ->
                    if (selectedId == null) list
                    else list.filter { it.categoryId == selectedId }
                }
                .take(5)

            TaskListUiState(
                tasks = tasks,
                filteredTasks = datedOrDone,
                undatedTasks = undated,
                agenda = agenda,
                agendaItems = agendaItems,
                suggestions = if (selectedId == null) agenda.suggestions else emptyList(),
                needsReview = needsReview,
                categories = categories,
                selectedCategoryId = selectedId,
                stats = stats,
                categoryNames = categories.associate { it.id as Long? to it.name },
                isLoading = false,
                error = null,
            )
        }.catch { e ->
            emit(
                TaskListUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load agenda",
                ),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TaskListUiState(),
        )

    fun selectCategory(categoryId: Long?) {
        selectedCategoryId.update { categoryId }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            completeTaskUseCase(task.id, completed = !task.isCompleted)
        }
    }

    fun toggleCompleteTaskId(taskId: Long) {
        viewModelScope.launch {
            val task = uiState.value.tasks.firstOrNull { it.id == taskId } ?: return@launch
            completeTaskUseCase(task.id, completed = !task.isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
            _events.emit(TaskListEvent.ShowUndoDelete(task.id, task.title))
        }
    }

    fun undoDelete(taskId: Long) {
        viewModelScope.launch {
            restoreTaskUseCase(taskId)
        }
    }

    fun exportTasks() {
        viewModelScope.launch {
            try {
                exportTasksUseCase()
                _events.emit(TaskListEvent.ShowMessage("Exported to Downloads"))
            } catch (e: Exception) {
                _events.emit(TaskListEvent.ShowMessage(e.message ?: "Export failed"))
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _events.emit(TaskListEvent.ShowMessage(message))
        }
    }
}
