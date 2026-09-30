package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(id: Long): Flow<Task?> = repository.observeTask(id)
}
