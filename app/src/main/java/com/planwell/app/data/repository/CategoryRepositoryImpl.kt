package com.planwell.app.data.repository

import com.planwell.app.data.local.dao.CategoryDao
import com.planwell.app.data.mapper.toDomain
import com.planwell.app.data.mapper.toEntity
import com.planwell.app.domain.model.Category
import com.planwell.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategoriesWithCount().map { rows ->
            rows.map { it.category.toDomain(taskCount = it.taskCount) }
        }

    override fun observeCategory(id: Long): Flow<Category?> =
        categoryDao.observeWithCount(id).map { row ->
            row?.category?.toDomain(taskCount = row.taskCount)
        }

    override suspend fun getCategory(id: Long): Category? {
        val entity = categoryDao.getById(id) ?: return null
        return entity.toDomain(taskCount = categoryDao.getTaskCount(id))
    }

    override suspend fun createCategory(category: Category): Long =
        categoryDao.insert(category.copy(id = 0).toEntity())

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
}
