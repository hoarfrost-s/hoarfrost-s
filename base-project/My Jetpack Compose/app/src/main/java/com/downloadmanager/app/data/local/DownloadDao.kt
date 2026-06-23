package com.downloadmanager.app.data.local

import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

interface DownloadDao {
    fun getTaskById(id: String): Flow<DownloadTaskEntity?>
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>
    fun getActiveTasks(): Flow<List<DownloadTaskEntity>>
    fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTaskEntity>>
    fun getTasksByCategory(categoryId: String): Flow<List<DownloadTaskEntity>>
    fun getRecentTasks(limit: Int): Flow<List<DownloadTaskEntity>>
    suspend fun insertTask(task: DownloadTaskEntity)
    suspend fun updateTaskStatus(id: String, status: DownloadStatus, updatedAt: Long)
    suspend fun updateProgress(id: String, downloadedSize: Long, updatedAt: Long)
    suspend fun updateTask(task: DownloadTaskEntity)
    suspend fun deleteTask(id: String)
    suspend fun deleteCompletedTasks()
    fun getTotalDownloadedSize(): Flow<Long>
    fun getTodayDownloadCount(startOfDay: Long, endOfDay: Long): Flow<Long>
    fun getDownloadCountByCategory(): Flow<Map<String, Long>>
}
