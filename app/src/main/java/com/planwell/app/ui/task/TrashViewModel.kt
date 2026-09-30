package com.planwell.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.task.GetDeletedTasksUseCase
import com.planwell.app.domain.usecase.task.PermanentlyDeleteTaskUseCase
import com.planwell.app.domain.usecase.task.RestoreTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    getDeletedTasksUseCase: GetDeletedTasksUseCase,
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val permanentlyDeleteTaskUseCase: PermanentlyDeleteTaskUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDeletedTasksUseCase().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun restore(taskId: Long) {
        viewModelScope.launch { restoreTaskUseCase(taskId) }
    }

    fun permanentlyDelete(taskId: Long) {
        viewModelScope.launch { permanentlyDeleteTaskUseCase(taskId) }
    }
}
