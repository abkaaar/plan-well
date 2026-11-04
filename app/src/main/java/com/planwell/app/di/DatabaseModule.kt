package com.planwell.app.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.planwell.app.data.attachment.FileOpportunityAttachmentStore
import com.planwell.app.data.attachment.OpportunityAttachmentStore
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
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.AppDatabaseMigrations
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DB_NAME,
        )
            .addMigrations(
                AppDatabaseMigrations.MIGRATION_4_5,
                AppDatabaseMigrations.MIGRATION_5_6,
                AppDatabaseMigrations.MIGRATION_6_7,
                AppDatabaseMigrations.MIGRATION_7_8,
            )
            // V_min ≥ 4: app never shipped to Play with DB v1–3 (no store installs,
            // no git tags for those schemas). Destructive wipe of 1–3 is deliberate;
            // do not invent untestable 1→4 migrations. Preserve all v4+ upgrades.
            .fallbackToDestructiveMigrationFrom(1, 2, 3)
            .build()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideTaskFtsDao(db: AppDatabase): TaskFtsDao = db.taskFtsDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideOpportunityDao(db: AppDatabase): OpportunityDao = db.opportunityDao()

    @Provides
    fun provideOpportunityAttachmentDao(db: AppDatabase): OpportunityAttachmentDao =
        db.opportunityAttachmentDao()

    @Provides
    fun provideOpportunityDateDao(db: AppDatabase): OpportunityDateDao = db.opportunityDateDao()

    @Provides
    fun provideOpportunityTaskLinkDao(db: AppDatabase): OpportunityTaskLinkDao =
        db.opportunityTaskLinkDao()

    @Provides
    fun provideOpportunityCandidateDao(db: AppDatabase): OpportunityCandidateDao =
        db.opportunityCandidateDao()

    @Provides
    fun provideCallDao(db: AppDatabase): CallDao = db.callDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AttachmentModule {

    @Binds
    @Singleton
    abstract fun bindOpportunityAttachmentStore(
        impl: FileOpportunityAttachmentStore,
    ): OpportunityAttachmentStore
}
