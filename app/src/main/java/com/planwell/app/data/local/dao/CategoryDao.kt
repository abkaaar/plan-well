package com.planwell.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.planwell.app.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

data class CategoryWithTaskCount(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "task_count") val taskCount: Int,
)

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT c.*,
          (
            SELECT COUNT(*) FROM tasks t
            WHERE t.category_id = c.id AND t.is_deleted = 0
          ) AS task_count
        FROM categories c
        ORDER BY c.name COLLATE NOCASE ASC
        """,
    )
    fun observeCategoriesWithCount(): Flow<List<CategoryWithTaskCount>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query(
        """
        SELECT c.*,
          (
            SELECT COUNT(*) FROM tasks t
            WHERE t.category_id = c.id AND t.is_deleted = 0
          ) AS task_count
        FROM categories c
        WHERE c.id = :id
        """,
    )
    fun observeWithCount(id: Long): Flow<CategoryWithTaskCount?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE category_id = :categoryId AND is_deleted = 0
        """,
    )
    suspend fun getTaskCount(categoryId: Long): Int
}
