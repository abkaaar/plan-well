package com.planwell.app.data.mapper

import com.planwell.app.data.local.db.entity.CategoryEntity
import com.planwell.app.domain.model.Category

fun CategoryEntity.toDomain(taskCount: Int = 0): Category =
    Category(
        id = id,
        name = name,
        colorHex = colorHex,
        taskCount = taskCount,
    )

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        colorHex = colorHex,
    )
