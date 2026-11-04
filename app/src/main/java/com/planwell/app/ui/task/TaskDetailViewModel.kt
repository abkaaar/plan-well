package com.planwell.app.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.Reminder
import com.planwell.app.domain.model.RepeatType
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.reminder.CancelReminderUseCase
import com.planwell.app.domain.usecase.reminder.GetRemindersForTaskUseCase
import com.planwell.app.domain.usecase.reminder.ScheduleReminderUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.DeleteTaskUseCase
import com.planwell.app.domain.usecase.task.GetTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val categoryName: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val sourceOpportunity: Opportunity? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
    val reminderError: String? = null,
)

private data class TaskDetailFlags(
    val deleted: Boolean = false,
    val reminderError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTaskUseCase: GetTaskUseCase,
    getRemindersForTaskUseCase: GetRemindersForTaskUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    opportunityRepository: OpportunityRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase,
) : ViewModel() {

    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])
    private val flags = MutableStateFlow(TaskDetailFlags())

    private val sourceOpportunityFlow =
        opportunityRepository.observeOpportunityIdsForTask(taskId).flatMapLatest { ids ->
            val id = ids.firstOrNull()
            if (id == null) flowOf(null) else opportunityRepository.observeOpportunity(id)
        }

    private val coreFlow = combine(
        getTaskUseCase(taskId),
        getCategoriesUseCase(),
        getRemindersForTaskUseCase(taskId),
        sourceOpportunityFlow,
    ) { task, categories, reminders, opportunity ->
        Quad(task, categories.firstOrNull { it.id == task?.categoryId }?.name, reminders, opportunity)
    }

    val uiState: StateFlow<TaskDetailUiState> =
        combine(coreFlow, flags) { core, flagState ->
            TaskDetailUiState(
                task = core.task,
                categoryName = core.categoryName,
                reminders = core.reminders,
                sourceOpportunity = core.opportunity?.takeUnless { it.isDeleted },
                isLoading = false,
                deleted = flagState.deleted,
                reminderError = flagState.reminderError,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TaskDetailUiState(),
        )

    fun toggleComplete() {
        val task = uiState.value.task ?: return
        viewModelScope.launch {
            completeTaskUseCase(task.id, completed = !task.isCompleted)
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            flags.value = flags.value.copy(deleted = true)
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
                flags.value = flags.value.copy(reminderError = null)
            } catch (e: Exception) {
                flags.value = flags.value.copy(
                    reminderError = e.message ?: "Could not schedule reminder",
                )
            }
        }
    }

    fun cancelReminder(reminderId: Long) {
        viewModelScope.launch {
            cancelReminderUseCase(reminderId)
        }
    }

    private data class Quad(
        val task: Task?,
        val categoryName: String?,
        val reminders: List<Reminder>,
        val opportunity: Opportunity?,
    )
}
