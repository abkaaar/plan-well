package com.planwell.app.domain.usecase.category

import com.planwell.app.domain.model.Category
import com.planwell.app.domain.model.CategoryColorPresets
import com.planwell.app.domain.repository.CategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke() = repository.observeCategories()
}

class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(name: String, colorHex: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Name is required" }
        require(trimmed.length <= 40) { "Name max 40 characters" }
        val color = normalizeColor(colorHex)
        require(color in CategoryColorPresets.hexValues) { "Pick a preset colour" }
        return repository.createCategory(Category(name = trimmed, colorHex = color))
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Long, name: String, colorHex: String) {
        require(id > 0) { "Invalid category" }
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Name is required" }
        require(trimmed.length <= 40) { "Name max 40 characters" }
        val color = normalizeColor(colorHex)
        require(color in CategoryColorPresets.hexValues) { "Pick a preset colour" }
        repository.updateCategory(Category(id = id, name = trimmed, colorHex = color))
    }
}

private fun normalizeColor(hex: String): String =
    hex.trim().uppercase().let { if (it.startsWith("#")) it else "#$it" }

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteCategory(id)
    }
}
