package com.downloadmanager.data.di

import com.downloadmanager.data.datastore.SettingsDataStore
import com.downloadmanager.data.repository.CategoryRepositoryImpl
import com.downloadmanager.data.repository.TaskRepositoryImpl
import com.downloadmanager.domain.repository.CategoryRepository
import com.downloadmanager.domain.repository.ConfigRepository
import com.downloadmanager.domain.repository.TaskRepository
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
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: SettingsDataStore): ConfigRepository
}