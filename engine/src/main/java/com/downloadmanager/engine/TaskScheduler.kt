package com.downloadmanager.engine

import com.downloadmanager.common.DownloadProgress
import com.downloadmanager.common.DownloadTask
import kotlinx.coroutines.flow.Flow

interface TaskScheduler {
    suspend fun submit(task: DownloadTask)
    fun setMaxConcurrency(max: Int)
    fun getActiveCount(): Int
    fun getQueuedCount(): Int
    suspend fun pauseTask(taskId: String)
    suspend fun resumeTask(taskId: String)
    suspend fun cancelTask(taskId: String)
    suspend fun pauseAll()
    suspend fun resumeAll()
    suspend fun restoreTasks(tasks: List<DownloadTask>)
    fun getProgressFlow(): Flow<DownloadProgress>
}