package com.planwell.app.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.reminder.CancelReminderUseCase
import com.planwell.app.domain.usecase.reminder.GetRemindersForTaskUseCase
import com.planwell.app.domain.usecase.reminder.ScheduleReminderUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.DeleteTaskUseCase
import com.planwell.app.domain.usecase.task.GetTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val categoryName: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
    val reminderError: String? = null,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTaskUseCase: GetTaskUseCase,
    getRemindersForTaskUseCase: GetRemindersForTaskUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase,
) : ViewModel() {

    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getTaskUseCase(taskId),
                getCategoriesUseCase(),
            ) { task, categories ->
                task to categories.firstOrNull { it.id == task?.categoryId }?.name
            }.collect { (task, categoryName) ->
                _uiState.update {
                    it.copy(task = task, categoryName = categoryName, isLoading = false)
                }
            }
        }
        viewModelScope.launch {
            getRemindersForTaskUseCase(taskId).collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun toggleComplete() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            completeTaskUseCase(task.id, completed = !task.isCompleted)
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    fun scheduleReminder(triggerAtMs: Long, repeatType: RepeatType) {
        viewModelScope.launch {
            try {
                scheduleReminderUseCase(
                    Reminder(
                        taskId = taskId,
                        triggerAtMs = triggerAtMs,
                        repeatType = repeatType,
                    ),
                )
                _uiState.update { it.copy(reminderError = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(reminderError = e.message ?: "Could not schedule reminder")
                }
            }
        }
    }

    fun cancelReminder(reminderId: Long) {
        viewModelScope.launch {
            cancelReminderUseCase(reminderId)
        }
    }
}
