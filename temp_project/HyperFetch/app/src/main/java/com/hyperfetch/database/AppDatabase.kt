package com.hyperfetch.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hyperfetch.dao.DownloadTaskDao
import com.hyperfetch.dao.TaskChunkDao
import com.hyperfetch.entity.ConfigEntity
import com.hyperfetch.entity.DownloadTaskEntity
import com.hyperfetch.entity.TaskChunkEntity

/**
 * 应用数据库
 */
@Database(
    entities = [
        DownloadTaskEntity::class,
        TaskChunkEntity::class,
        ConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun taskChunkDao(): TaskChunkDao

    companion object {
        private const val DATABASE_NAME = "hyperfetch_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
