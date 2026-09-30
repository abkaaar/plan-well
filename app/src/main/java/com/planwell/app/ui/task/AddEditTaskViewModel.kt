package com.planwell.app.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.Priority
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.task.CreateTaskUseCase
import com.planwell.app.domain.usecase.task.GetTaskUseCase
import com.planwell.app.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditTaskUiState(
    val taskId: Long = 0,
    val title: String = "",
    val description: String = "",
    val dueDateMs: Long? = null,
    val priority: Priority = Priority.NONE,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    getTaskUseCase: GetTaskUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val form = MutableStateFlow(
        AddEditTaskUiState(taskId = taskId, isEditing = taskId > 0),
    )

    val uiState: StateFlow<AddEditTaskUiState> =
        combine(form, getCategoriesUseCase()) { state, categories ->
            state.copy(categories = categories)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            form.value,
        )

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                getTaskUseCase(taskId).collect { task ->
                    if (task != null) {
                        form.update {
                            it.copy(
                                title = task.title,
                                description = task.description.orEmpty(),
                                dueDateMs = task.dueDateMs,
                                priority = task.priority,
                                categoryId = task.categoryId,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        form.update { it.copy(title = value.take(100), error = null) }
    }

    fun onDescriptionChange(value: String) {
        form.update { it.copy(description = value.take(500), error = null) }
    }

    fun onDueDateChange(value: Long?) {
        form.update { it.copy(dueDateMs = value) }
    }

    fun onPriorityChange(value: Priority) {
        form.update { it.copy(priority = value) }
    }

    fun onCategoryChange(value: Long?) {
        form.update { it.copy(categoryId = value) }
    }

    fun save() {
        val state = form.value
        if (state.title.isBlank()) {
            form.update { it.copy(error = "Title is required") }
            return
        }
        viewModelScope.launch {
            form.update { it.copy(isSaving = true, error = null) }
            try {
                val task = Task(
                    id = state.taskId,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    dueDateMs = state.dueDateMs,
                    priority = state.priority,
                    categoryId = state.categoryId,
                )
                if (state.isEditing) {
                    updateTaskUseCase(task)
                } else {
                    createTaskUseCase(task)
                }
                form.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                form.update {
                    it.copy(isSaving = false, error = e.message ?: "Could not save task")
                }
            }
        }
    }
}
