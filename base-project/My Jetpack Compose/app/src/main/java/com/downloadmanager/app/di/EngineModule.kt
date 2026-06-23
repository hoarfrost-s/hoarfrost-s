package com.downloadmanager.app.di

import com.downloadmanager.app.engine.DownloadEngine
import com.downloadmanager.app.engine.DownloadEngineImpl
import com.downloadmanager.app.engine.TokenBucket
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideGlobalTokenBucket(): TokenBucket {
        return TokenBucket(0)
    }

    @Provides
    @Singleton
    fun provideDownloadEngine(
        downloadEngineImpl: DownloadEngineImpl
    ): DownloadEngine {
        return downloadEngineImpl
    }
}
