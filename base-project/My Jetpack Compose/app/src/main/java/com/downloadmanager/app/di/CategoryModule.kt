package com.downloadmanager.app.di

import com.downloadmanager.app.category.CategoryMatcher
import com.downloadmanager.app.category.CategoryMatcherImpl
import com.downloadmanager.app.data.local.CategoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoryModule {

    @Provides
    @Singleton
    fun provideCategoryMatcher(categoryDao: CategoryDao): CategoryMatcher {
        return CategoryMatcherImpl(categoryDao)
    }
}
