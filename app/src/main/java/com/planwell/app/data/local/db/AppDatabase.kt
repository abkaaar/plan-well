package com.planwell.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.planwell.app.data.local.dao.CallDao
import com.planwell.app.data.local.dao.CategoryDao
import com.planwell.app.data.local.dao.OpportunityAttachmentDao
import com.planwell.app.data.local.dao.OpportunityCandidateDao
import com.planwell.app.data.local.dao.OpportunityDao
import com.planwell.app.data.local.dao.OpportunityDateDao
import com.planwell.app.data.local.dao.OpportunityTaskLinkDao
import com.planwell.app.data.local.dao.ReminderDao
import com.planwell.app.data.local.dao.TaskDao
import com.planwell.app.data.local.dao.TaskFtsDao
import com.planwell.app.data.local.db.entity.CallEntity
import com.planwell.app.data.local.db.entity.CategoryEntity
import com.planwell.app.data.local.db.entity.OpportunityAttachmentEntity
import com.planwell.app.data.local.db.entity.OpportunityCandidateEntity
import com.planwell.app.data.local.db.entity.OpportunityDateEntity
import com.planwell.app.data.local.db.entity.OpportunityEntity
import com.planwell.app.data.local.db.entity.OpportunityTaskLinkEntity
import com.planwell.app.data.local.db.entity.ReminderEntity
import com.planwell.app.data.local.db.entity.TaskEntity
import com.planwell.app.data.local.db.entity.TaskFtsEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        CategoryEntity::class,
        TaskFtsEntity::class,
        OpportunityEntity::class,
        OpportunityAttachmentEntity::class,
        OpportunityDateEntity::class,
        OpportunityTaskLinkEntity::class,
        OpportunityCandidateEntity::class,
        CallEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskFtsDao(): TaskFtsDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun opportunityDao(): OpportunityDao
    abstract fun opportunityAttachmentDao(): OpportunityAttachmentDao
    abstract fun opportunityDateDao(): OpportunityDateDao
    abstract fun opportunityTaskLinkDao(): OpportunityTaskLinkDao
    abstract fun opportunityCandidateDao(): OpportunityCandidateDao
    abstract fun callDao(): CallDao

    companion object {
        const val DB_NAME = "plan_well_db"
    }
}
