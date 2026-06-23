package com.downloadmanager.app.di

import android.content.Context
import com.downloadmanager.app.data.DownloadDatabase
import com.downloadmanager.app.data.local.CategoryDao
import com.downloadmanager.app.data.local.CategoryDaoImpl
import com.downloadmanager.app.data.local.DownloadDao
import com.downloadmanager.app.data.local.DownloadDaoImpl
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
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
    fun provideSqlDriver(@ApplicationContext context: Context): SqlDriver {
        return AndroidSqliteDriver(
            schema = DownloadDatabase.Schema,
            context = context,
            name = "download_manager.db"
        )
    }

    @Provides
    @Singleton
    fun provideDownloadDatabase(driver: SqlDriver): DownloadDatabase {
        return DownloadDatabase(driver)
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: DownloadDatabase): DownloadDao {
        return DownloadDaoImpl(database)
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: DownloadDatabase): CategoryDao {
        return CategoryDaoImpl(database)
    }
}
