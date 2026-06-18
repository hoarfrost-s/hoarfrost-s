package com.hyperfetch.repo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 应用数据库类
 * Room数据库的主入口，管理所有实体和DAO
 */
@Database(entities = {
        DownloadTaskEntity.class,
        TaskChunkEntity.class,
        ConfigEntity.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    
    /**
     * 数据库实例
     */
    private static volatile AppDatabase instance;
    
    /**
     * 数据库名称
     */
    private static final String DATABASE_NAME = "hyperfetch_db";
    
    /**
     * 应用上下文（用于 Worker 中获取数据库实例）
     */
    private static Context applicationContext;
    
    /**
     * 获取下载任务DAO
     * @return 下载任务DAO
     */
    public abstract DownloadTaskDao getDownloadTaskDao();
    
    /**
     * 获取任务分块DAO
     * @return 任务分块DAO
     */
    public abstract TaskChunkDao getTaskChunkDao();
    
    /**
     * 获取配置DAO
     * @return 配置DAO
     */
    public abstract ConfigDao getConfigDao();
    
    /**
     * 获取数据库实例（单例模式）
     * @param context 应用上下文
     * @return 数据库实例
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    // 保存应用上下文
                    if (applicationContext == null) {
                        applicationContext = context.getApplicationContext();
                    }
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
    
    /**
     * 使用已保存的应用上下文获取数据库实例
     * 需要先调用 getInstance(Context) 初始化
     * @param context 应用上下文
     * @return 数据库实例
     */
    public static AppDatabase getApplicationContext(Context context) {
        return getInstance(context);
    }
}