package com.planwell.app.domain.repository

import com.planwell.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeCategory(id: Long): Flow<Category?>
    suspend fun getCategory(id: Long): Category?
    suspend fun createCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
}
