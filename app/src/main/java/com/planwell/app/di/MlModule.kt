package com.planwell.app.di

import com.planwell.app.data.ml.ManagedTextRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * On-device ML Kit Latin text recognition. The recognizer client is created lazily
 * inside [ManagedTextRecognizer] with TextRecognizerOptions.DEFAULT_OPTIONS.
 */
@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideManagedTextRecognizer(): ManagedTextRecognizer = ManagedTextRecognizer()
}
