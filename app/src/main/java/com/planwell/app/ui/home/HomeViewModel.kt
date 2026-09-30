package com.planwell.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
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
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val exportTasksUseCase: ExportTasksUseCase,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    private val _events = MutableSharedFlow<TaskListEvent>()
    val events: SharedFlow<TaskListEvent> = _events.asSharedFlow()

    val uiState: StateFlow<TaskListUiState> =
        combine(
            getTasksUseCase(),
            getCategoriesUseCase(),
            completionStatsUseCase(),
            selectedCategoryId,
        ) { tasks, categories, stats, selectedId ->
            val filtered = if (selectedId == null) {
                tasks
            } else {
                tasks.filter { it.categoryId == selectedId }
            }
            TaskListUiState(
                tasks = tasks,
                filteredTasks = filtered,
                categories = categories,
                selectedCategoryId = selectedId,
                stats = stats,
                categoryNames = categories.associate { it.id as Long? to it.name },
                isLoading = false,
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
}
