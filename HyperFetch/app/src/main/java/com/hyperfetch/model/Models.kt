package com.hyperfetch.model

import java.util.Date

/**
 * 下载任务数据类
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val totalSize: Long = 0L,
    val downloaded: Long = 0L,
    val category: Category = Category.OTHER,
    val status: TaskStatus = TaskStatus.QUEUED,
    val priority: Priority = Priority.NORMAL,
    val threadCount: Int = 4,
    val speedLimit: Long = Long.MAX_VALUE, // Long.MAX_VALUE 表示无限制
    val savePath: String = "",
    val createTime: Date = Date(),
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val eta: Long = 0L, // 预计剩余时间（秒）
    val speed: Long = 0L // 当前速度（字节/秒）
) {
    val progress: Float
        get() = if (totalSize > 0) downloaded.toFloat() / totalSize else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isResumable: Boolean
        get() = status == TaskStatus.PAUSED || status == TaskStatus.QUEUED

    val isDownloading: Boolean
        get() = status == TaskStatus.DOWNLOADING

    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val isFailed: Boolean
        get() = status == TaskStatus.FAILED
}

/**
 * 任务分块数据类
 */
data class TaskChunk(
    val taskId: String,
    val index: Int,
    val start: Long,
    val end: Long,
    val downloaded: Long = 0L,
    val status: ChunkStatus = ChunkStatus.PENDING
) {
    val size: Long
        get() = end - start + 1

    val isCompleted: Boolean
        get() = status == ChunkStatus.COMPLETED

    val remainingSize: Long
        get() = size - downloaded
}

/**
 * 调度器配置
 */
data class SchedulerConfig(
    val maxConcurrentTasks: Int = 3,
    val multiTaskEnabled: Boolean = true,
    val queueStrategy: QueueStrategy = QueueStrategy.PRIORITY_FIRST,
    val wifiOnlyEnabled: Boolean = false
)

/**
 * 资源元数据
 */
data class ResourceMeta(
    val totalSize: Long,
    val supportRange: Boolean,
    val contentType: String?,
    val fileName: String?
)

/**
 * 下载选项
 */
data class DownloadOptions(
    val threadCount: Int = 4,
    val speedLimit: Long = Long.MAX_VALUE,
    val priority: Priority = Priority.NORMAL,
    val startNow: Boolean = true,
    val savePath: String = "",
    val category: Category? = null // null 表示自动分类
)
