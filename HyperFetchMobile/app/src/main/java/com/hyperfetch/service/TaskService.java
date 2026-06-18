package com.hyperfetch.service;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyperfetch.classify.CategoryService;
import com.hyperfetch.engine.DownloadEngine;
import com.hyperfetch.model.Category;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.Priority;
import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.repo.DownloadTaskEntity;
import com.hyperfetch.repo.TaskRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务服务类
 * 提供任务的创建、启动、暂停、删除、重试等核心操作
 * 以及任务配置和查询功能
 */
public class TaskService {

    private static final String TAG = "TaskService";

    /**
     * 最小线程数
     */
    private static final int MIN_THREAD_COUNT = 1;

    /**
     * 最大线程数
     */
    private static final int MAX_THREAD_COUNT = 9;

    /**
     * 最小并发任务数
     */
    private static final int MIN_CONCURRENT_TASKS = 1;

    /**
     * 最大并发任务数
     */
    private static final int MAX_CONCURRENT_TASKS = 5;

    /**
     * 默认最大并发任务数
     */
    private static final int DEFAULT_MAX_CONCURRENT_TASKS = 3;

    /**
     * 应用上下文
     */
    private final Context context;

    /**
     * 任务数据仓库
     */
    private final TaskRepository repository;

    /**
     * 下载引擎
     */
    private final DownloadEngine downloadEngine;

    /**
     * 分类服务
     */
    private CategoryService categoryService;

    /**
     * 内存中的任务缓存
     */
    private final Map<String, DownloadTask> taskCache;

    /**
     * 任务列表LiveData
     */
    private final MutableLiveData<List<DownloadTask>> taskListLiveData;

    /**
     * 全局限速（字节/秒），0表示不限制
     */
    private volatile long globalSpeedLimit = 0;

    /**
     * 最大并发任务数
     */
    private volatile int maxConcurrentTasks = DEFAULT_MAX_CONCURRENT_TASKS;

    /**
     * 线程池，用于异步操作
     */
    private final ExecutorService executor;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public TaskService(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new TaskRepository(this.context);
        this.downloadEngine = new DownloadEngine();
        this.taskCache = new ConcurrentHashMap<>();
        this.taskListLiveData = new MutableLiveData<>(new ArrayList<>());
        this.executor = Executors.newCachedThreadPool();

        // 初始化文件分类器
        initFileClassifier();

        // 设置下载引擎回调
        setupEngineCallbacks();

        // 加载已保存的任务
        loadSavedTasks();
    }

    /**
     * 初始化分类服务
     */
    private void initFileClassifier() {
        // 使用已有的CategoryService实现
        this.categoryService = new CategoryService();
    }

    /**
     * 设置下载引擎回调
     */
    private void setupEngineCallbacks() {
        downloadEngine.setTaskStateCallback(new DownloadEngine.TaskStateCallback() {
            @Override
            public void onTaskCompleted(String taskId) {
                handleTaskCompleted(taskId);
            }

            @Override
            public void onTaskFailed(String taskId, String error) {
                handleTaskFailed(taskId, error);
            }
        });
    }

    /**
     * 加载已保存的任务
     */
    private void loadSavedTasks() {
        executor.execute(() -> {
            // 从数据库加载任务并更新缓存
            // 实际实现中需要从repository获取数据
            publishTaskList();
        });
    }

    /**
     * 发布任务列表更新
     */
    private void publishTaskList() {
        List<DownloadTask> tasks = new ArrayList<>(taskCache.values());
        taskListLiveData.postValue(tasks);
    }

    // ==================== 核心方法 ====================

    /**
     * 创建新任务
     * 自动根据文件名分类
     *
     * @param url     下载地址
     * @param options 任务选项
     * @return 任务ID
     */
    public String createTask(String url, TaskOptions options) {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString();

        // 从URL提取文件名
        String fileName = extractFileName(url);
        if (options.getSavePath() == null || options.getSavePath().isEmpty()) {
            throw new IllegalArgumentException("保存路径不能为空");
        }

        // 创建任务对象
        DownloadTask task = new DownloadTask(taskId, url, fileName, options.getSavePath());
        task.setThreadCount(options.getThreadCount());
        task.setSpeedLimit(options.getSpeedLimit());
        task.setPriority(options.getPriority());
        task.setCreateTime(new Date());

        // 自动分类（使用URL进行分类）
        Category category = categoryService.classify(url);
        task.setCategory(category);

        // 设置初始状态
        task.setStatus(TaskStatus.QUEUED);

        // 保存到缓存
        taskCache.put(taskId, task);

        // 保存到数据库
        saveTaskToDatabase(task);

        // 发布任务列表更新
        publishTaskList();

        // 如果设置了立即开始，则启动任务
        if (options.isStartImmediately()) {
            start(taskId);
        }

        return taskId;
    }

    /**
     * 启动任务
     * 状态转换：QUEUED/PAUSED -> DOWNLOADING
     *
     * @param taskId 任务ID
     */
    public void start(String taskId) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        TaskStatus currentStatus = task.getStatus();
        if (currentStatus == TaskStatus.QUEUED || currentStatus == TaskStatus.PAUSED) {
            transitionStatus(taskId, currentStatus, TaskStatus.DOWNLOADING);
            downloadEngine.submit(task);
            updateTaskInDatabase(task);
        }
    }

    /**
     * 暂停任务
     * 状态转换：DOWNLOADING -> PAUSED
     *
     * @param taskId 任务ID
     */
    public void pause(String taskId) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        TaskStatus currentStatus = task.getStatus();
        if (currentStatus == TaskStatus.DOWNLOADING) {
            transitionStatus(taskId, currentStatus, TaskStatus.PAUSED);
            downloadEngine.pause(taskId);
            updateTaskInDatabase(task);
        }
    }

    /**
     * 删除任务
     *
     * @param taskId      任务ID
     * @param deleteFile  是否删除已下载的文件
     */
    public void delete(String taskId, boolean deleteFile) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 取消下载
        downloadEngine.cancel(taskId);

        // 如果需要删除文件
        if (deleteFile) {
            File file = new File(task.getSavePath(), task.getFileName());
            if (file.exists()) {
                file.delete();
            }
        }

        // 从缓存中移除
        taskCache.remove(taskId);

        // 从数据库中删除
        repository.deleteTaskById(taskId);

        // 发布任务列表更新
        publishTaskList();
    }

    /**
     * 重试失败的任务
     * 状态转换：FAILED -> DOWNLOADING
     *
     * @param taskId 任务ID
     */
    public void retry(String taskId) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        TaskStatus currentStatus = task.getStatus();
        if (currentStatus == TaskStatus.FAILED) {
            // 重置已下载大小
            task.setDownloaded(0);
            transitionStatus(taskId, currentStatus, TaskStatus.DOWNLOADING);
            downloadEngine.submit(task);
            updateTaskInDatabase(task);
        }
    }

    // ==================== 配置方法 ====================

    /**
     * 热调整线程数
     * 范围：[1, 9]
     *
     * @param taskId 任务ID
     * @param n      线程数
     */
    public void setThreadCount(String taskId, int n) {
        if (n < MIN_THREAD_COUNT || n > MAX_THREAD_COUNT) {
            throw new IllegalArgumentException("线程数必须在 " + MIN_THREAD_COUNT + " 到 " + MAX_THREAD_COUNT + " 之间");
        }

        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        task.setThreadCount(n);
        downloadEngine.setThreadCount(taskId, n);
        updateTaskInDatabase(task);
    }

    /**
     * 设置单任务限速
     *
     * @param taskId 任务ID
     * @param bps    限速（字节/秒），0表示不限制
     */
    public void setSpeedLimit(String taskId, long bps) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        task.setSpeedLimit(bps);
        // 更新引擎中的限速器
        // 实际实现中需要调用downloadEngine的相关方法
        updateTaskInDatabase(task);
    }

    /**
     * 设置全局限速
     *
     * @param bps 限速（字节/秒），0表示不限制
     */
    public void setGlobalSpeedLimit(long bps) {
        this.globalSpeedLimit = bps;
        // 实际实现中需要更新全局限速器
    }

    /**
     * 热调整并发任务数
     * 范围：[1, 5]
     *
     * @param n 并发任务数
     */
    public void setMaxConcurrentTasks(int n) {
        if (n < MIN_CONCURRENT_TASKS || n > MAX_CONCURRENT_TASKS) {
            throw new IllegalArgumentException("并发任务数必须在 " + MIN_CONCURRENT_TASKS + " 到 " + MAX_CONCURRENT_TASKS + " 之间");
        }

        this.maxConcurrentTasks = n;
        // 实际实现中需要更新调度器配置
    }

    // ==================== 查询方法 ====================

    /**
     * 获取所有任务列表
     *
     * @return 任务列表的LiveData
     */
    public LiveData<List<DownloadTask>> list() {
        return taskListLiveData;
    }

    /**
     * 按分类查询任务
     *
     * @param category 文件分类
     * @return 任务列表的LiveData
     */
    public LiveData<List<DownloadTask>> list(Category category) {
        MutableLiveData<List<DownloadTask>> filteredLiveData = new MutableLiveData<>();
        taskListLiveData.observeForever(tasks -> {
            List<DownloadTask> filtered = new ArrayList<>();
            for (DownloadTask task : tasks) {
                if (task.getCategory() == category) {
                    filtered.add(task);
                }
            }
            filteredLiveData.setValue(filtered);
        });
        return filteredLiveData;
    }

    /**
     * 根据ID获取任务
     *
     * @param taskId 任务ID
     * @return 任务对象，不存在返回null
     */
    public DownloadTask getById(String taskId) {
        return taskCache.get(taskId);
    }

    // ==================== 状态机驱动 ====================

    /**
     * 状态转换
     * 验证转换合法性并更新状态
     *
     * @param taskId 任务ID
     * @param from   当前状态
     * @param to     目标状态
     */
    private void transitionStatus(String taskId, TaskStatus from, TaskStatus to) {
        DownloadTask task = taskCache.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 验证状态转换是否合法
        if (!StatusTransition.canTransition(from, to)) {
            throw new IllegalStateException(
                    "非法状态转换: " + from + " -> " + to);
        }

        // 更新状态
        task.setStatus(to);

        // 发布任务列表更新
        publishTaskList();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从URL提取文件名
     *
     * @param url 下载地址
     * @return 文件名
     */
    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown_file";
        }

        try {
            String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            int lastSlash = decodedUrl.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < decodedUrl.length() - 1) {
                String name = decodedUrl.substring(lastSlash + 1);
                // 移除查询参数
                int queryIndex = name.indexOf('?');
                if (queryIndex > 0) {
                    name = name.substring(0, queryIndex);
                }
                return name.isEmpty() ? "download_" + System.currentTimeMillis() : name;
            }
        } catch (Exception e) {
            // 忽略解码错误
        }

        return "download_" + System.currentTimeMillis();
    }

    /**
     * 保存任务到数据库
     *
     * @param task 下载任务
     */
    private void saveTaskToDatabase(DownloadTask task) {
        executor.execute(() -> {
            DownloadTaskEntity entity = convertToEntity(task);
            repository.insertTask(entity);
        });
    }

    /**
     * 更新数据库中的任务
     *
     * @param task 下载任务
     */
    private void updateTaskInDatabase(DownloadTask task) {
        executor.execute(() -> {
            DownloadTaskEntity entity = convertToEntity(task);
            repository.updateTask(entity);
        });
    }

    /**
     * 将DownloadTask转换为DownloadTaskEntity
     *
     * @param task 下载任务
     * @return 数据库实体
     */
    private DownloadTaskEntity convertToEntity(DownloadTask task) {
        DownloadTaskEntity entity = new DownloadTaskEntity();
        entity.id = task.getId();
        entity.url = task.getUrl();
        entity.fileName = task.getFileName();
        entity.totalSize = task.getTotalSize();
        entity.downloaded = task.getDownloaded();
        entity.category = task.getCategory() != null ? task.getCategory().name() : Category.OTHER.name();
        entity.status = task.getStatus() != null ? task.getStatus().name() : TaskStatus.QUEUED.name();
        entity.priority = task.getPriority() != null ? task.getPriority().name() : Priority.NORMAL.name();
        entity.threadCount = task.getThreadCount();
        entity.speedLimit = task.getSpeedLimit();
        entity.savePath = task.getSavePath();
        entity.createTime = task.getCreateTime() != null ? task.getCreateTime().getTime() : System.currentTimeMillis();
        return entity;
    }

    /**
     * 处理任务完成
     *
     * @param taskId 任务ID
     */
    private void handleTaskCompleted(String taskId) {
        DownloadTask task = taskCache.get(taskId);
        if (task != null) {
            transitionStatus(taskId, task.getStatus(), TaskStatus.COMPLETED);
            updateTaskInDatabase(task);
        }
    }

    /**
     * 处理任务失败
     *
     * @param taskId 任务ID
     * @param error  错误信息
     */
    private void handleTaskFailed(String taskId, String error) {
        DownloadTask task = taskCache.get(taskId);
        if (task != null) {
            transitionStatus(taskId, task.getStatus(), TaskStatus.FAILED);
            updateTaskInDatabase(task);
        }
    }

    /**
     * 设置分类服务
     *
     * @param categoryService 分类服务
     */
    public void setCategoryService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取下载引擎
     *
     * @return 下载引擎
     */
    public DownloadEngine getDownloadEngine() {
        return downloadEngine;
    }

    /**
     * 获取全局限速
     *
     * @return 全局限速（字节/秒）
     */
    public long getGlobalSpeedLimit() {
        return globalSpeedLimit;
    }

    /**
     * 获取最大并发任务数
     *
     * @return 最大并发任务数
     */
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    /**
     * 关闭服务，释放资源
     */
    public void shutdown() {
        downloadEngine.shutdown();
        repository.shutdown();
        executor.shutdown();
    }
}