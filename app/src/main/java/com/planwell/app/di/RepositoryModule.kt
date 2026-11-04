package com.planwell.app.di

import com.planwell.app.data.contacts.ContactsRepositoryImpl
import com.planwell.app.data.repository.CallRepositoryImpl
import com.planwell.app.data.repository.CategoryRepositoryImpl
import com.planwell.app.data.repository.OpportunityRepositoryImpl
import com.planwell.app.data.repository.ReminderRepositoryImpl
import com.planwell.app.data.repository.TaskRepositoryImpl
import com.planwell.app.domain.repository.CallRepository
import com.planwell.app.domain.repository.CategoryRepository
import com.planwell.app.domain.repository.ContactsRepository
import com.planwell.app.domain.repository.OpportunityRepository
import com.planwell.app.domain.repository.ReminderRepository
import com.planwell.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindOpportunityRepository(impl: OpportunityRepositoryImpl): OpportunityRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(impl: CallRepositoryImpl): CallRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository
}
