package com.downloadmanager.domain.repository

import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAllTasks(): Flow<List<DownloadTask>>
    fun observeTasksByStatus(status: TaskStatus): Flow<List<DownloadTask>>
    suspend fun getTaskById(taskId: String): DownloadTask?
    suspend fun insertTask(task: DownloadTask)
    suspend fun updateTask(task: DownloadTask)
    suspend fun deleteTask(taskId: String)
    suspend fun deleteTasks(taskIds: List<String>)
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, status: TaskStatus)
    suspend fun getQueuedTasks(): List<DownloadTask>
}