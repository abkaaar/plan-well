package com.planwell.app.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.usecase.category.CreateCategoryUseCase
import com.planwell.app.domain.usecase.category.DeleteCategoryUseCase
import com.planwell.app.domain.usecase.category.GetCategoriesUseCase
import com.planwell.app.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun save(id: Long, name: String, colorHex: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (id > 0) {
                    updateCategoryUseCase(id, name, colorHex)
                } else {
                    createCategoryUseCase(name, colorHex)
                }
                _uiState.update { it.copy(error = null) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Could not save category")
                }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteCategoryUseCase(id)
        }
    }
}
