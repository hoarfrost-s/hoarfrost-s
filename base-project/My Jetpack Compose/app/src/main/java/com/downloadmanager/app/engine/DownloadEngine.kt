package com.downloadmanager.app.engine

import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    fun enqueue(config: DownloadTaskConfig): String

    fun pause(taskId: String)

    fun resume(taskId: String)

    fun cancel(taskId: String, deleteFile: Boolean)

    fun observeProgress(taskId: String): Flow<DownloadProgress>

    fun setGlobalSpeedLimit(bytesPerSecond: Long)

    fun setMaxConcurrentTasks(maxConcurrent: Int)

    fun getActiveTaskCount(): Int

    fun getPendingTaskCount(): Int
}
