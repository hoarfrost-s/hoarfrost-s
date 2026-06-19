package com.hyperfetch.event

import com.hyperfetch.model.Category
import com.hyperfetch.model.TaskStatus

/**
 * 任务创建事件
 */
data class TaskCreatedEvent(
    val taskId: String,
    val url: String,
    val fileName: String,
    val category: Category
)

/**
 * 下载进度事件
 */
data class ProgressEvent(
    val taskId: String,
    val downloaded: Long,
    val total: Long,
    val speed: Long,
    val eta: Long // 预计剩余时间（秒）
) {
    val progress: Float
        get() = if (total > 0) downloaded.toFloat() / total else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}

/**
 * 状态变更事件
 */
data class StatusChangedEvent(
    val taskId: String,
    val fromStatus: TaskStatus,
    val toStatus: TaskStatus
)

/**
 * 错误事件
 */
data class ErrorEvent(
    val taskId: String,
    val cause: String,
    val retryCount: Int
)

/**
 * 任务完成事件
 */
data class CompletedEvent(
    val taskId: String,
    val filePath: String,
    val fileName: String
)

/**
 * 调度器配置变更事件
 */
data class SchedulerConfigChangedEvent(
    val maxConcurrentTasks: Int,
    val multiTaskEnabled: Boolean
)

/**
 * 速度更新事件（聚合所有任务速度）
 */
data class TotalSpeedEvent(
    val totalSpeed: Long,
    val downloadingCount: Int,
    val queuedCount: Int
)

/**
 * Wi-Fi 状态变更事件
 */
data class WifiStateEvent(
    val isWifiConnected: Boolean
)
