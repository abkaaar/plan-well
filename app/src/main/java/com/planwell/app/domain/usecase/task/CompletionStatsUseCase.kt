package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.model.CompletionStats
import com.planwell.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class CompletionStatsUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(nowMs: Long = System.currentTimeMillis()): Flow<CompletionStats> {
        val (start, end) = dayBounds(nowMs)
        return repository.observeCompletionStats(start, end)
    }

    companion object {
        fun dayBounds(nowMs: Long): Pair<Long, Long> {
            val start = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis
            return start to end
        }
    }
}
