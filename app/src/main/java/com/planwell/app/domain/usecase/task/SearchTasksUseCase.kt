package com.planwell.app.domain.usecase.task

import com.planwell.app.domain.model.Task
import com.planwell.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(rawQuery: String): Flow<List<Task>> {
        val ftsQuery = sanitizeFtsQuery(rawQuery)
        if (ftsQuery.isBlank()) return flowOf(emptyList())
        return repository.searchTasks(ftsQuery)
    }

    companion object {
        /** Strip FTS operators and append prefix wildcards for typeahead search. */
        fun sanitizeFtsQuery(raw: String): String {
            val cleaned = raw.trim()
                .replace(Regex("""["'*]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (cleaned.isBlank()) return ""
            return cleaned.split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { token -> "$token*" }
        }
    }
}
