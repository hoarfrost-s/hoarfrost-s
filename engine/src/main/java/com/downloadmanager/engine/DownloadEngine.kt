package com.downloadmanager.engine

import com.downloadmanager.common.DownloadProgress
import com.downloadmanager.common.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    suspend fun execute(task: DownloadTask): Flow<DownloadProgress>
    suspend fun pause(taskId: String)
    suspend fun resume(taskId: String)
    suspend fun cancel(taskId: String)
}