package com.planwell.app.di

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.planwell.app.data.extract.HeuristicEntityExtractor
import com.planwell.app.domain.extract.EntityExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExtractProvidesModule {
    @Provides
    @Singleton
    fun providePhoneNumberUtil(): PhoneNumberUtil = PhoneNumberUtil.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractBindModule {
    @Binds
    @Singleton
    abstract fun bindEntityExtractor(impl: HeuristicEntityExtractor): EntityExtractor
}
