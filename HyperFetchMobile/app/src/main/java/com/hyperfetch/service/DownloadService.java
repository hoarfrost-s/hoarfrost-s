package com.hyperfetch.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hyperfetch.engine.DownloadConfig;
import com.hyperfetch.engine.DownloadEngine;
import com.hyperfetch.event.ProgressEvent;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.repo.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 下载前台服务
 * 负责管理下载任务的执行，提供前台通知防止系统回收
 */
public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    /**
     * 前台通知ID
     */
    private static final int NOTIFICATION_ID = 1001;

    /**
     * 通知渠道ID
     */
    private static final String NOTIFICATION_CHANNEL_ID = "download_service_channel";

    /**
     * 通知渠道名称
     */
    private static final String NOTIFICATION_CHANNEL_NAME = "下载服务";

    /**
     * 下载引擎
     */
    private DownloadEngine engine;

    /**
     * 任务数据仓库
     */
    private TaskRepository taskRepository;

    /**
     * 服务绑定器
     */
    private DownloadServiceBinder binder;

    /**
     * 任务映射表（任务ID -> 下载任务）
     */
    private final Map<String, DownloadTask> taskMap = new ConcurrentHashMap<>();

    /**
     * 进度监听器列表
     */
    private final List<DownloadServiceBinder.ProgressListener> progressListeners = new CopyOnWriteArrayList<>();

    /**
     * 任务状态监听器列表
     */
    private final List<DownloadServiceBinder.TaskStateListener> taskStateListeners = new CopyOnWriteArrayList<>();

    /**
     * 服务是否正在运行
     */
    private volatile boolean isRunning = false;

    /**
     * 通知管理器
     */
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化通知管理器
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道
        createNotificationChannel();

        // 启动前台通知，防止系统回收
        Notification notification = buildForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);

        // 初始化下载引擎
        DownloadConfig config = new DownloadConfig();
        engine = new DownloadEngine(config);

        // 初始化任务仓库
        taskRepository = new TaskRepository(getApplicationContext());

        // 设置引擎回调
        setupEngineCallbacks();

        // 创建绑定器
        binder = new DownloadServiceBinder(this);

        // 标记服务正在运行
        isRunning = true;

        // 初始化默认配置
        taskRepository.initDefaultConfig();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        // 处理不同的动作
        switch (action) {
            case ServiceActions.ACTION_CREATE:
                handleCreateAction(intent);
                break;
            case ServiceActions.ACTION_PAUSE:
                handlePauseAction(intent);
                break;
            case ServiceActions.ACTION_RESUME:
                handleResumeAction(intent);
                break;
            case ServiceActions.ACTION_DELETE:
                handleDeleteAction(intent);
                break;
            case ServiceActions.ACTION_RETRY:
                handleRetryAction(intent);
                break;
            case ServiceActions.ACTION_CANCEL:
                handleCancelAction(intent);
                break;
            case ServiceActions.ACTION_RECOVER:
                handleRecoverAction(intent);
                break;
            default:
                break;
        }

        // 被杀后自动重启
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        // 关闭下载引擎
        if (engine != null) {
            engine.shutdown();
        }

        // 关闭任务仓库
        if (taskRepository != null) {
            taskRepository.shutdown();
        }

        // 清理资源
        taskMap.clear();
        progressListeners.clear();
        taskStateListeners.clear();
    }

    // ==================== 动作处理方法 ====================

    /**
     * 处理创建任务动作
     *
     * @param intent Intent对象
     */
    private void handleCreateAction(Intent intent) {
        String url = intent.getStringExtra(ServiceActions.KEY_URL);
        String fileName = intent.getStringExtra(ServiceActions.KEY_FILE_NAME);
        String savePath = intent.getStringExtra(ServiceActions.KEY_SAVE_PATH);

        if (url == null || url.isEmpty()) {
            return;
        }

        createTask(url, fileName, savePath);
    }

    /**
     * 处理暂停任务动作
     *
     * @param intent Intent对象
     */
    private void handlePauseAction(Intent intent) {
        String taskId = intent.getStringExtra(ServiceActions.KEY_TASK_ID);
        if (taskId != null) {
            pauseTask(taskId);
        }
    }

    /**
     * 处理恢复任务动作
     *
     * @param intent Intent对象
     */
    private void handleResumeAction(Intent intent) {
        String taskId = intent.getStringExtra(ServiceActions.KEY_TASK_ID);
        if (taskId != null) {
            resumeTask(taskId);
        }
    }

    /**
     * 处理删除任务动作
     *
     * @param intent Intent对象
     */
    private void handleDeleteAction(Intent intent) {
        String taskId = intent.getStringExtra(ServiceActions.KEY_TASK_ID);
        if (taskId != null) {
            deleteTask(taskId);
        }
    }

    /**
     * 处理重试任务动作
     *
     * @param intent Intent对象
     */
    private void handleRetryAction(Intent intent) {
        String taskId = intent.getStringExtra(ServiceActions.KEY_TASK_ID);
        if (taskId != null) {
            retryTask(taskId);
        }
    }

    /**
     * 处理取消任务动作
     *
     * @param intent Intent对象
     */
    private void handleCancelAction(Intent intent) {
        String taskId = intent.getStringExtra(ServiceActions.KEY_TASK_ID);
        if (taskId != null) {
            cancelTask(taskId);
        }
    }

    /**
     * 处理恢复任务动作
     * 用于进程被杀后的自动恢复
     *
     * @param intent Intent对象
     */
    private void handleRecoverAction(Intent intent) {
        int recoveryCount = intent.getIntExtra(ServiceActions.KEY_RECOVERY_COUNT, 0);
        // 恢复未完成的下载任务
        recoverUnfinishedTasks();
    }

    /**
     * 恢复未完成的下载任务
     * 从数据库加载 DOWNLOADING 和 PAUSED 状态的任务
     */
    private void recoverUnfinishedTasks() {
        new Thread(() -> {
            try {
                // 从数据库查询未完成的任务
                java.util.List<com.hyperfetch.repo.DownloadTaskEntity> unfinishedTasks =
                        taskRepository.getUnfinishedTasksFromDao();

                if (unfinishedTasks == null || unfinishedTasks.isEmpty()) {
                    return;
                }

                // 将任务转换为 DownloadTask 对象并恢复
                for (com.hyperfetch.repo.DownloadTaskEntity entity : unfinishedTasks) {
                    DownloadTask task = convertEntityToTask(entity);
                    if (task != null) {
                        // 添加到任务映射表
                        taskMap.put(task.getId(), task);
                        // 提交给引擎恢复下载
                        engine.submit(task);
                    }
                }

                // 更新通知
                updateNotification();

            } catch (Exception e) {
                // 恢复失败，记录日志
            }
        }).start();
    }

    /**
     * 将数据库实体转换为 DownloadTask 对象
     *
     * @param entity 数据库实体
     * @return DownloadTask 对象
     */
    private DownloadTask convertEntityToTask(com.hyperfetch.repo.DownloadTaskEntity entity) {
        if (entity == null) {
            return null;
        }

        DownloadTask task = new DownloadTask();
        task.setId(entity.id);
        task.setUrl(entity.url);
        task.setFileName(entity.fileName);
        task.setSavePath(entity.savePath);
        task.setTotalSize(entity.totalSize);
        task.setDownloaded(entity.downloaded);
        task.setThreadCount(entity.threadCount);
        task.setSpeedLimit(entity.speedLimit);

        // 设置状态为 QUEUED（等待重新下载）
        task.setStatus(TaskStatus.QUEUED);

        return task;
    }

    // ==================== 公共API方法 ====================

    /**
     * 创建下载任务
     *
     * @param url      下载地址
     * @param fileName 文件名
     * @param savePath 保存路径
     * @return 任务ID
     */
    public String createTask(String url, String fileName, String savePath) {
        String taskId = generateTaskId();
        DownloadTask task = new DownloadTask(taskId, url, fileName, savePath);
        return createTask(task);
    }

    /**
     * 创建下载任务
     *
     * @param task 下载任务对象
     * @return 任务ID
     */
    public String createTask(DownloadTask task) {
        if (task == null || task.getUrl() == null) {
            return null;
        }

        // 确保任务ID存在
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(generateTaskId());
        }

        // 添加到任务映射表
        taskMap.put(task.getId(), task);

        // 更新任务状态
        String oldState = null;
        String newState = TaskStatus.QUEUED.name();
        task.setStatus(TaskStatus.QUEUED);

        // 通知状态变化
        notifyTaskStateChanged(task.getId(), oldState, newState);

        // 提交给下载引擎
        engine.submit(task);

        return task.getId();
    }

    /**
     * 暂停下载任务
     *
     * @param taskId 任务ID
     */
    public void pauseTask(String taskId) {
        DownloadTask task = taskMap.get(taskId);
        if (task == null) {
            return;
        }

        String oldState = task.getStatus().name();

        // 调用引擎暂停
        engine.pause(taskId);

        // 更新状态
        task.setStatus(TaskStatus.PAUSED);
        String newState = TaskStatus.PAUSED.name();

        // 通知状态变化
        notifyTaskStateChanged(taskId, oldState, newState);

        // 更新通知
        updateNotification();
    }

    /**
     * 恢复下载任务
     *
     * @param taskId 任务ID
     */
    public void resumeTask(String taskId) {
        DownloadTask task = taskMap.get(taskId);
        if (task == null) {
            return;
        }

        String oldState = task.getStatus().name();

        // 调用引擎恢复
        engine.resume(taskId);

        // 更新状态
        task.setStatus(TaskStatus.DOWNLOADING);
        String newState = TaskStatus.DOWNLOADING.name();

        // 通知状态变化
        notifyTaskStateChanged(taskId, oldState, newState);

        // 更新通知
        updateNotification();
    }

    /**
     * 删除下载任务
     *
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        DownloadTask task = taskMap.get(taskId);
        if (task == null) {
            return;
        }

        String oldState = task.getStatus().name();

        // 调用引擎取消
        engine.cancel(taskId);

        // 从映射表中移除
        taskMap.remove(taskId);

        // 通知状态变化
        notifyTaskStateChanged(taskId, oldState, "DELETED");

        // 更新通知
        updateNotification();
    }

    /**
     * 重试下载任务
     *
     * @param taskId 任务ID
     */
    public void retryTask(String taskId) {
        DownloadTask task = taskMap.get(taskId);
        if (task == null) {
            return;
        }

        String oldState = task.getStatus().name();

        // 重置任务状态
        task.setStatus(TaskStatus.QUEUED);
        task.setDownloaded(0);

        // 重新提交给引擎
        engine.submit(task);

        String newState = TaskStatus.QUEUED.name();

        // 通知状态变化
        notifyTaskStateChanged(taskId, oldState, newState);

        // 更新通知
        updateNotification();
    }

    /**
     * 取消下载任务
     *
     * @param taskId 任务ID
     */
    public void cancelTask(String taskId) {
        deleteTask(taskId);
    }

    /**
     * 获取所有下载任务
     *
     * @return 任务列表
     */
    public List<DownloadTask> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }

    /**
     * 根据ID获取下载任务
     *
     * @param taskId 任务ID
     * @return 下载任务，如果不存在返回null
     */
    public DownloadTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    // ==================== 监听器管理方法 ====================

    /**
     * 注册进度监听器
     *
     * @param listener 进度监听器
     */
    public void registerProgressListener(DownloadServiceBinder.ProgressListener listener) {
        if (listener != null && !progressListeners.contains(listener)) {
            progressListeners.add(listener);
        }
    }

    /**
     * 注销进度监听器
     *
     * @param listener 进度监听器
     */
    public void unregisterProgressListener(DownloadServiceBinder.ProgressListener listener) {
        progressListeners.remove(listener);
    }

    /**
     * 注册任务状态监听器
     *
     * @param listener 任务状态监听器
     */
    public void registerTaskStateListener(DownloadServiceBinder.TaskStateListener listener) {
        if (listener != null && !taskStateListeners.contains(listener)) {
            taskStateListeners.add(listener);
        }
    }

    /**
     * 注销任务状态监听器
     *
     * @param listener 任务状态监听器
     */
    public void unregisterTaskStateListener(DownloadServiceBinder.TaskStateListener listener) {
        taskStateListeners.remove(listener);
    }

    // ==================== 配置方法 ====================

    /**
     * 设置任务线程数
     *
     * @param taskId      任务ID
     * @param threadCount 线程数
     */
    public void setThreadCount(String taskId, int threadCount) {
        engine.setThreadCount(taskId, threadCount);
    }

    /**
     * 设置任务速度限制
     *
     * @param taskId     任务ID
     * @param speedLimit 速度限制（字节/秒）
     */
    public void setSpeedLimit(String taskId, long speedLimit) {
        DownloadTask task = taskMap.get(taskId);
        if (task != null) {
            task.setSpeedLimit(speedLimit);
        }
    }

    /**
     * 检查服务是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("下载服务通知渠道");
            channel.setShowBadge(false);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 构建前台通知
     *
     * @return 通知对象
     */
    private Notification buildForegroundNotification() {
        // 获取下载中的任务数量
        int downloadingCount = 0;
        for (DownloadTask task : taskMap.values()) {
            if (task.getStatus() == TaskStatus.DOWNLOADING) {
                downloadingCount++;
            }
        }

        String contentText = downloadingCount > 0
                ? String.format("正在下载 %d 个任务", downloadingCount)
                : "下载服务运行中";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("HyperFetch 下载服务")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false);

        return builder.build();
    }

    /**
     * 更新通知
     */
    private void updateNotification() {
        if (notificationManager != null) {
            Notification notification = buildForegroundNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 设置引擎回调
     */
    private void setupEngineCallbacks() {
        // 设置进度回调
        engine.setProgressCallback(event -> {
            // 通知所有进度监听器
            for (DownloadServiceBinder.ProgressListener listener : progressListeners) {
                listener.onProgress(event);
            }

            // 更新任务进度
            DownloadTask task = taskMap.get(event.getTaskId());
            if (task != null) {
                task.setDownloaded(event.getDownloaded());
                task.setTotalSize(event.getTotalSize());
            }
        });

        // 设置任务状态回调
        engine.setTaskStateCallback(new DownloadEngine.TaskStateCallback() {
            @Override
            public void onTaskCompleted(String taskId) {
                DownloadTask task = taskMap.get(taskId);
                if (task != null) {
                    String oldState = task.getStatus().name();
                    task.setStatus(TaskStatus.COMPLETED);
                    String newState = TaskStatus.COMPLETED.name();

                    // 通知状态变化
                    notifyTaskStateChanged(taskId, oldState, newState);
                }

                // 通知所有任务状态监听器
                for (DownloadServiceBinder.TaskStateListener listener : taskStateListeners) {
                    listener.onTaskCompleted(taskId);
                }

                // 更新通知
                updateNotification();
            }

            @Override
            public void onTaskFailed(String taskId, String error) {
                DownloadTask task = taskMap.get(taskId);
                if (task != null) {
                    String oldState = task.getStatus().name();
                    task.setStatus(TaskStatus.FAILED);
                    String newState = TaskStatus.FAILED.name();

                    // 通知状态变化
                    notifyTaskStateChanged(taskId, oldState, newState);
                }

                // 通知所有任务状态监听器
                for (DownloadServiceBinder.TaskStateListener listener : taskStateListeners) {
                    listener.onTaskFailed(taskId, error);
                }

                // 更新通知
                updateNotification();
            }
        });
    }

    /**
     * 通知任务状态变化
     *
     * @param taskId   任务ID
     * @param oldState 旧状态
     * @param newState 新状态
     */
    private void notifyTaskStateChanged(String taskId, String oldState, String newState) {
        for (DownloadServiceBinder.TaskStateListener listener : taskStateListeners) {
            listener.onTaskStateChanged(taskId, oldState, newState);
        }
    }

    /**
     * 生成任务ID
     *
     * @return 任务ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}