package com.hyperfetch.service

import android.content.Context
import android.os.Environment
import com.hyperfetch.engine.DownloadEngine
import com.hyperfetch.event.*
import com.hyperfetch.model.*
import com.hyperfetch.repo.TaskRepository
import com.hyperfetch.scheduler.TaskScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 任务服务
 * 核心服务层，处理任务 CRUD、状态流转
 */
class TaskService(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = TaskRepository(context)
    private lateinit var engine: DownloadEngine
    private lateinit var scheduler: TaskScheduler

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val _totalSpeed = MutableStateFlow(0L)
    val totalSpeed: StateFlow<Long> = _totalSpeed.asStateFlow()

    private val _downloadingCount = MutableStateFlow(0)
    val downloadingCount: StateFlow<Int> = _downloadingCount.asStateFlow()

    private val _schedulerConfig = MutableStateFlow(SchedulerConfig())
    val schedulerConfig: StateFlow<SchedulerConfig> = _schedulerConfig.asStateFlow()

    // 回调
    var onTaskStatusChanged: ((String, TaskStatus, TaskStatus) -> Unit)? = null

    /**
     * 初始化服务
     */
    fun initialize() {
        engine = DownloadEngine(context)
        scheduler = TaskScheduler(engine) { taskId, from, to ->
            scope.launch {
                onTaskStatusChanged?.invoke(taskId, from, to)
            }
        }

        // 订阅事件
        EventBusWrapper.register(this)

        // 加载任务
        scope.launch {
            repository.getAllTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }

        // 恢复未完成的任务
        restorePendingTasks()
    }

    /**
     * 恢复挂起的任务
     */
    private fun restorePendingTasks() {
        scope.launch {
            val pendingTasks = repository.getPendingTasks()
            pendingTasks.forEach { task ->
                if (task.status == TaskStatus.PAUSED || task.status == TaskStatus.QUEUED) {
                    scheduler.enqueue(task)
                }
            }
        }
    }

    /**
     * 创建下载任务
     */
    suspend fun createTask(url: String, options: DownloadOptions = DownloadOptions()): String {
        val savePath = options.savePath.ifEmpty { getDefaultSavePath() }
        val task = repository.createTask(url, options, savePath)

        EventBusWrapper.postMain(
            TaskCreatedEvent(
                taskId = task.id,
                url = task.url,
                fileName = task.fileName,
                category = task.category
            )
        )

        if (options.startNow) {
            scheduler.enqueue(task)
        }

        return task.id
    }

    /**
     * 开始任务
     */
    suspend fun start(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return
        val newStatus = TaskStatus.DOWNLOADING

        repository.updateTaskStatus(taskId, newStatus)
        scheduler.enqueue(task)

        EventBusWrapper.postMain(
            StatusChangedEvent(taskId, task.status, newStatus)
        )
    }

    /**
     * 暂停任务
     */
    suspend fun pause(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return
        val newStatus = TaskStatus.PAUSED

        engine.pause(taskId)
        repository.updateTaskStatus(taskId, newStatus)
        scheduler.removeTask(taskId)

        EventBusWrapper.postMain(
            StatusChangedEvent(taskId, task.status, newStatus)
        )
    }

    /**
     * 恢复任务
     */
    suspend fun resume(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return
        val newStatus = TaskStatus.DOWNLOADING

        repository.updateTaskStatus(taskId, newStatus)
        scheduler.enqueue(task)

        EventBusWrapper.postMain(
            StatusChangedEvent(taskId, task.status, newStatus)
        )
    }

    /**
     * 删除任务
     */
    suspend fun delete(taskId: String, deleteFile: Boolean = false) {
        engine.pause(taskId)
        scheduler.removeTask(taskId)

        if (deleteFile) {
            repository.deleteTaskAndFile(taskId)
        } else {
            repository.deleteTask(taskId)
        }
    }

    /**
     * 重试任务
     */
    suspend fun retry(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return
        val newStatus = TaskStatus.QUEUED

        repository.updateTaskError(taskId, TaskStatus.QUEUED, null)
        scheduler.enqueue(task.copy(status = newStatus))

        EventBusWrapper.postMain(
            StatusChangedEvent(taskId, task.status, newStatus)
        )
    }

    /**
     * 设置线程数
     */
    suspend fun setThreadCount(taskId: String, count: Int) {
        require(count in 1..9) { "Thread count must be between 1 and 9" }
        val task = repository.getTaskById(taskId) ?: return

        // 更新数据库
        val updatedTask = task.copy(threadCount = count)
        // 注意：这里需要更新实体
    }

    /**
     * 设置速度限制
     */
    suspend fun setSpeedLimit(taskId: String, bytesPerSecond: Long) {
        engine.adjustSpeedLimit(taskId, bytesPerSecond)
    }

    /**
     * 设置全局限速
     */
    fun setGlobalSpeedLimit(bytesPerSecond: Long) {
        // 应用到所有活动任务
        _tasks.value.filter { it.isDownloading }.forEach { task ->
            scope.launch {
                setSpeedLimit(task.id, bytesPerSecond)
            }
        }
    }

    /**
     * 设置最大并发任务数
     */
    suspend fun setMaxConcurrentTasks(count: Int) {
        require(count in 1..5) { "Concurrent tasks must be between 1 and 5" }
        scheduler.setMaxConcurrentTasks(count)
        _schedulerConfig.value = _schedulerConfig.value.copy(maxConcurrentTasks = count)
    }

    /**
     * 设置多任务开关
     */
    suspend fun setMultiTaskEnabled(enabled: Boolean) {
        scheduler.setMultiTaskEnabled(enabled)
        _schedulerConfig.value = _schedulerConfig.value.copy(multiTaskEnabled = enabled)
    }

    /**
     * 设置排队策略
     */
    suspend fun setQueueStrategy(strategy: QueueStrategy) {
        scheduler.setQueueStrategy(strategy)
        _schedulerConfig.value = _schedulerConfig.value.copy(queueStrategy = strategy)
    }

    /**
     * 获取默认保存路径
     */
    private fun getDefaultSavePath(): String {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "HyperFetch").absolutePath
    }

    /**
     * 获取任务
     */
    suspend fun getTask(taskId: String): DownloadTask? {
        return repository.getTaskById(taskId)
    }

    /**
     * 获取分类统计
     */
    fun getCategoryStats(): Map<Category, Int> {
        return _tasks.value.groupBy { it.category }
            .mapValues { it.value.size }
    }

    /**
     * 销毁服务
     */
    fun destroy() {
        engine.stopAll()
        scheduler.clear()
        EventBusWrapper.unregister(this)
    }
}

/**
 * EventBus 订阅者接口
 */
@Suppress("UNUSED_PARAMETER")
class EventSubscriber(private val service: TaskService) {

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onProgressEvent(event: ProgressEvent) {
        // 更新速度统计
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onCompletedEvent(event: CompletedEvent) {
        // 任务完成处理
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onStatusChangedEvent(event: StatusChangedEvent) {
        // 状态变更处理
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onErrorEvent(event: ErrorEvent) {
        // 错误处理
    }
}
