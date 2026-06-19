package com.downloadmanager.data.repository

import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FileCategory
import com.downloadmanager.common.LinkInfo
import com.downloadmanager.common.TaskStatus
import com.downloadmanager.data.local.DownloadTaskEntity
import com.downloadmanager.data.local.TaskDao
import com.downloadmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<DownloadTask>> {
        return taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<DownloadTask>> {
        return taskDao.observeByStatus(status.name).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getTaskById(taskId: String): DownloadTask? {
        return taskDao.getById(taskId)?.toDomain()
    }

    override suspend fun insertTask(task: DownloadTask) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun updateTask(task: DownloadTask) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.delete(taskId)
    }

    override suspend fun deleteTasks(taskIds: List<String>) {
        taskDao.deleteAll(taskIds)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        taskDao.updateStatus(taskId, status.name)
    }

    override suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, status: TaskStatus) {
        taskDao.updateProgress(taskId, downloadedBytes, status.name)
    }

    override suspend fun getQueuedTasks(): List<DownloadTask> {
        return taskDao.getQueuedTasks().map { it.toDomain() }
    }

    private fun DownloadTaskEntity.toDomain(): DownloadTask {
        return DownloadTask(
            id = id,
            url = url,
            fileName = fileName,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            category = try { FileCategory.valueOf(category) } catch (e: Exception) { FileCategory.OTHER },
            status = try { TaskStatus.valueOf(status) } catch (e: Exception) { TaskStatus.WAITING },
            threadCount = threadCount,
            savePath = savePath,
            createdAt = createdAt,
            queuePosition = queuePosition,
            errorMessage = errorMessage,
            linkInfo = LinkInfo(
                url = url,
                fileName = fileName,
                fileSize = fileSize,
                contentType = contentType,
                category = try { FileCategory.valueOf(category) } catch (e: Exception) { FileCategory.OTHER },
                threadCount = threadCount,
                savePath = savePath,
                createdAt = createdAt
            )
        )
    }

    private fun DownloadTask.toEntity(): DownloadTaskEntity {
        return DownloadTaskEntity(
            id = id,
            url = url,
            fileName = fileName,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            category = category.name,
            status = status.name,
            threadCount = threadCount,
            savePath = savePath,
            createdAt = createdAt,
            queuePosition = queuePosition,
            errorMessage = errorMessage,
            contentType = linkInfo?.contentType
        )
    }
}