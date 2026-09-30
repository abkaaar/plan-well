package com.planwell.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Task
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.task.CompleteTaskUseCase
import com.planwell.app.domain.usecase.task.SearchTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val categoryNames: Map<Long?, String> = emptyMap(),
    val isSearching: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTasksUseCase: SearchTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val categoryNames: StateFlow<Map<Long?, String>> =
        getCategoriesUseCase()
            .map { list -> list.associate { it.id as Long? to it.name } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val uiState: StateFlow<SearchUiState> =
        combine(
            query,
            query
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { raw ->
                    searchTasksUseCase(raw)
                },
            categoryNames,
        ) { rawQuery, results, names ->
            SearchUiState(
                query = rawQuery,
                results = results,
                categoryNames = names,
                isSearching = false,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SearchUiState(),
        )

    fun onQueryChange(value: String) {
        query.update { value }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            completeTaskUseCase(task.id, completed = !task.isCompleted)
        }
    }
}
