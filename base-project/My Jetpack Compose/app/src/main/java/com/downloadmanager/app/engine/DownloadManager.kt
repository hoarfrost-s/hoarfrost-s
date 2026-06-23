package com.downloadmanager.app.engine

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val downloadEngine: DownloadEngine
) {

    fun startDownload(
        url: String,
        fileName: String,
        savePath: String,
        tempPath: String,
        threadCount: Int = 3,
        speedLimit: Long = 0
    ): String {
        val config = DownloadTaskConfig(
            url = url,
            fileName = fileName,
            savePath = savePath,
            tempPath = tempPath,
            threadCount = threadCount,
            speedLimit = speedLimit
        )
        return downloadEngine.enqueue(config)
    }

    fun startDownload(config: DownloadTaskConfig): String {
        return downloadEngine.enqueue(config)
    }

    fun pauseDownload(taskId: String) {
        downloadEngine.pause(taskId)
    }

    fun resumeDownload(taskId: String) {
        downloadEngine.resume(taskId)
    }

    fun cancelDownload(taskId: String, deleteFile: Boolean = true) {
        downloadEngine.cancel(taskId, deleteFile)
    }

    fun observeProgress(taskId: String): Flow<DownloadProgress> {
        return downloadEngine.observeProgress(taskId)
    }

    fun setGlobalSpeedLimit(bytesPerSecond: Long) {
        downloadEngine.setGlobalSpeedLimit(bytesPerSecond)
    }

    fun setMaxConcurrentTasks(maxConcurrent: Int) {
        downloadEngine.setMaxConcurrentTasks(maxConcurrent)
    }

    fun getActiveTaskCount(): Int {
        return downloadEngine.getActiveTaskCount()
    }

    fun getPendingTaskCount(): Int {
        return downloadEngine.getPendingTaskCount()
    }

    fun getTotalTaskCount(): Int {
        return getActiveTaskCount() + getPendingTaskCount()
    }

    fun pauseAll() {
    }

    fun resumeAll() {
    }

    fun cancelAll(deleteFile: Boolean = true) {
    }
}
