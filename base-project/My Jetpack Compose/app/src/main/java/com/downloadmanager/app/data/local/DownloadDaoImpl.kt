package com.downloadmanager.app.data.local

import com.downloadmanager.app.data.DownloadDatabase
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadDaoImpl(
    private val database: DownloadDatabase
) : DownloadDao {

    private val queries get() = database.downloadTaskQueries

    override fun getTaskById(id: String): Flow<DownloadTaskEntity?> {
        return queries.getTaskById(id)
            .asFlow()
            .mapToOneOrNull()
            .map { it?.toEntity() }
    }

    override fun getAllTasks(): Flow<List<DownloadTaskEntity>> {
        return queries.getAllTasks()
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getActiveTasks(): Flow<List<DownloadTaskEntity>> {
        return queries.getActiveTasks()
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTaskEntity>> {
        return queries.getTasksByStatus(status.value)
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getTasksByCategory(categoryId: String): Flow<List<DownloadTaskEntity>> {
        return queries.getTasksByCategory(categoryId)
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getRecentTasks(limit: Int): Flow<List<DownloadTaskEntity>> {
        return queries.getRecentTasks(limit.toLong())
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override suspend fun insertTask(task: DownloadTaskEntity) {
        queries.insertTask(
            id = task.id,
            url = task.url,
            file_name = task.fileName,
            original_name = task.originalName,
            total_size = task.totalSize,
            downloaded_size = task.downloadedSize,
            thread_count = task.threadCount.toLong(),
            speed_limit = task.speedLimit,
            status = task.status.value,
            category_id = task.categoryId,
            sub_category_id = task.subCategoryId,
            save_path = task.savePath,
            temp_path = task.tempPath,
            mime_type = task.mimeType,
            md5_hash = task.md5Hash,
            error_message = task.errorMessage,
            created_at = task.createdAt,
            updated_at = task.updatedAt,
            completed_at = task.completedAt
        )
    }

    override suspend fun updateTaskStatus(id: String, status: DownloadStatus, updatedAt: Long) {
        queries.updateTaskStatus(
            status = status.value,
            updated_at = updatedAt,
            id = id
        )
    }

    override suspend fun updateProgress(id: String, downloadedSize: Long, updatedAt: Long) {
        queries.updateProgress(
            downloaded_size = downloadedSize,
            updated_at = updatedAt,
            id = id
        )
    }

    override suspend fun updateTask(task: DownloadTaskEntity) {
        queries.updateTask(
            url = task.url,
            file_name = task.fileName,
            original_name = task.originalName,
            total_size = task.totalSize,
            downloaded_size = task.downloadedSize,
            thread_count = task.threadCount.toLong(),
            speed_limit = task.speedLimit,
            status = task.status.value,
            category_id = task.categoryId,
            sub_category_id = task.subCategoryId,
            save_path = task.savePath,
            temp_path = task.tempPath,
            mime_type = task.mimeType,
            md5_hash = task.md5Hash,
            error_message = task.errorMessage,
            updated_at = task.updatedAt,
            completed_at = task.completedAt,
            id = task.id
        )
    }

    override suspend fun deleteTask(id: String) {
        queries.deleteTask(id)
    }

    override suspend fun deleteCompletedTasks() {
        queries.deleteCompletedTasks()
    }

    override fun getTotalDownloadedSize(): Flow<Long> {
        return queries.getTotalDownloadedSize()
            .asFlow()
            .mapToOneOrNull()
            .map { it ?: 0L }
    }

    override fun getTodayDownloadCount(startOfDay: Long, endOfDay: Long): Flow<Long> {
        return queries.getTodayDownloadCount(startOfDay, endOfDay)
            .asFlow()
            .mapToOneOrNull()
            .map { it ?: 0L }
    }

    override fun getDownloadCountByCategory(): Flow<Map<String, Long>> {
        return queries.getDownloadCountByCategory()
            .asFlow()
            .mapToList()
            .map { list ->
                list.associate { 
                    (it.category_id ?: "unknown") to (it.count ?: 0L)
                }
            }
    }

    private fun com.downloadmanager.app.data.Download_task.toEntity(): DownloadTaskEntity {
        return DownloadTaskEntity(
            id = id,
            url = url,
            fileName = file_name,
            originalName = original_name,
            totalSize = total_size,
            downloadedSize = downloaded_size,
            threadCount = thread_count.toInt(),
            speedLimit = speed_limit,
            status = DownloadStatus.fromValue(status),
            categoryId = category_id,
            subCategoryId = sub_category_id,
            savePath = save_path,
            tempPath = temp_path,
            mimeType = mime_type,
            md5Hash = md5_hash,
            errorMessage = error_message,
            createdAt = created_at,
            updatedAt = updated_at,
            completedAt = completed_at
        )
    }
}
