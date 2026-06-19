package com.downloadmanager.engine.di

import com.downloadmanager.engine.DefaultDownloadEngine
import com.downloadmanager.engine.DefaultTaskScheduler
import com.downloadmanager.engine.DownloadEngine
import com.downloadmanager.engine.RateLimiter
import com.downloadmanager.engine.TaskScheduler
import com.downloadmanager.engine.TokenBucketRateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRateLimiter(): RateLimiter {
        return TokenBucketRateLimiter(0) // No limit by default
    }

    @Provides
    @Singleton
    fun provideDownloadEngine(
        okHttpClient: OkHttpClient,
        rateLimiter: RateLimiter
    ): DownloadEngine {
        return DefaultDownloadEngine(okHttpClient, rateLimiter)
    }

    @Provides
    @Singleton
    fun provideTaskScheduler(engine: DownloadEngine): TaskScheduler {
        return DefaultTaskScheduler(engine, 3)
    }
}