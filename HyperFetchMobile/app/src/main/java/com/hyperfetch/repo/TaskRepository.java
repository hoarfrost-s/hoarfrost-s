package com.hyperfetch.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务数据仓库类
 * 封装所有DAO操作，提供业务方法
 */
public class TaskRepository {
    
    /**
     * 下载任务DAO
     */
    private final DownloadTaskDao downloadTaskDao;
    
    /**
     * 任务分块DAO
     */
    private final TaskChunkDao taskChunkDao;
    
    /**
     * 配置DAO
     */
    private final ConfigDao configDao;
    
    /**
     * 线程池，用于执行数据库操作
     */
    private final ExecutorService executor;
    
    /**
     * 构造函数
     * @param context 应用上下文
     */
    public TaskRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        downloadTaskDao = database.getDownloadTaskDao();
        taskChunkDao = database.getTaskChunkDao();
        configDao = database.getConfigDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // ==================== 下载任务操作 ====================
    
    /**
     * 获取所有下载任务
     * @return 任务列表的LiveData
     */
    public LiveData<List<DownloadTaskEntity>> getAllTasks() {
        return downloadTaskDao.getAll();
    }
    
    /**
     * 根据分类获取下载任务
     * @param category 分类名称
     * @return 任务列表的LiveData
     */
    public LiveData<List<DownloadTaskEntity>> getTasksByCategory(String category) {
        return downloadTaskDao.getByCategory(category);
    }
    
    /**
     * 根据ID获取下载任务
     * @param id 任务ID
     * @return 任务实体
     */
    public DownloadTaskEntity getTaskById(String id) {
        return downloadTaskDao.getById(id);
    }
    
    /**
     * 插入下载任务（异步）
     * @param task 任务实体
     */
    public void insertTask(DownloadTaskEntity task) {
        executor.execute(() -> downloadTaskDao.insert(task));
    }
    
    /**
     * 更新下载任务（异步）
     * @param task 任务实体
     */
    public void updateTask(DownloadTaskEntity task) {
        executor.execute(() -> downloadTaskDao.update(task));
    }
    
    /**
     * 删除下载任务（异步）
     * @param task 任务实体
     */
    public void deleteTask(DownloadTaskEntity task) {
        executor.execute(() -> downloadTaskDao.delete(task));
    }
    
    /**
     * 根据ID删除下载任务（异步）
     * @param taskId 任务ID
     */
    public void deleteTaskById(String taskId) {
        executor.execute(() -> {
            DownloadTaskEntity task = downloadTaskDao.getById(taskId);
            if (task != null) {
                downloadTaskDao.delete(task);
                taskChunkDao.deleteByTaskId(taskId);
            }
        });
    }

    /**
     * 获取所有未完成的下载任务（DOWNLOADING 和 PAUSED 状态）
     * 用于进程被杀后的自动恢复
     * @return 未完成任务列表
     */
    public java.util.List<DownloadTaskEntity> getUnfinishedTasksFromDao() {
        return downloadTaskDao.getUnfinishedTasks();
    }

    // ==================== 任务分块操作 ====================
    
    /**
     * 根据任务ID获取所有分块
     * @param taskId 任务ID
     * @return 分块列表
     */
    public List<TaskChunkEntity> getChunksByTaskId(String taskId) {
        return taskChunkDao.getByTaskId(taskId);
    }
    
    /**
     * 批量插入分块（异步）
     * @param chunks 分块列表
     */
    public void insertChunks(List<TaskChunkEntity> chunks) {
        executor.execute(() -> taskChunkDao.insertAll(chunks));
    }
    
    /**
     * 更新分块（异步）
     * @param chunk 分块实体
     */
    public void updateChunk(TaskChunkEntity chunk) {
        executor.execute(() -> taskChunkDao.update(chunk));
    }
    
    /**
     * 根据任务ID删除所有分块（异步）
     * @param taskId 任务ID
     */
    public void deleteChunksByTaskId(String taskId) {
        executor.execute(() -> taskChunkDao.deleteByTaskId(taskId));
    }
    
    // ==================== 配置操作 ====================
    
    /**
     * 获取调度器配置
     * @return 配置实体
     */
    public ConfigEntity getConfig() {
        return configDao.getConfig();
    }
    
    /**
     * 保存配置（异步）
     * @param config 配置实体
     */
    public void saveConfig(ConfigEntity config) {
        executor.execute(() -> {
            ConfigEntity existing = configDao.getConfig();
            if (existing == null) {
                configDao.insert(config);
            } else {
                configDao.update(config);
            }
        });
    }
    
    /**
     * 更新配置（异步）
     * @param config 配置实体
     */
    public void updateConfig(ConfigEntity config) {
        executor.execute(() -> configDao.update(config));
    }
    
    /**
     * 初始化默认配置（如果不存在）
     */
    public void initDefaultConfig() {
        executor.execute(() -> {
            ConfigEntity existing = configDao.getConfig();
            if (existing == null) {
                ConfigEntity defaultConfig = new ConfigEntity();
                defaultConfig.id = 1;
                defaultConfig.maxConcurrentTasks = 3;
                defaultConfig.multiTaskEnabled = true;
                defaultConfig.queueStrategy = "FIFO";
                defaultConfig.wifiOnly = false;
                defaultConfig.defaultThreadCount = 3;
                configDao.insert(defaultConfig);
            }
        });
    }
    
    /**
     * 关闭仓库，释放资源
     */
    public void shutdown() {
        executor.shutdown();
    }
}