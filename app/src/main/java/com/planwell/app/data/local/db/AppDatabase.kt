package com.planwell.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.planwell.app.data.local.dao.CategoryDao
import com.planwell.app.data.local.dao.ReminderDao
import com.planwell.app.data.local.dao.TaskDao
import com.planwell.app.data.local.dao.TaskFtsDao
import com.planwell.app.data.local.db.entity.CategoryEntity
import com.planwell.app.data.local.db.entity.ReminderEntity
import com.planwell.app.data.local.db.entity.TaskEntity
import com.planwell.app.data.local.db.entity.TaskFtsEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        CategoryEntity::class,
        TaskFtsEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskFtsDao(): TaskFtsDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DB_NAME = "plan_well_db"
    }
}
