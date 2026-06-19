package com.hyperfetch.repo

import android.content.Context
import com.hyperfetch.classify.CategoryService
import com.hyperfetch.dao.DownloadTaskDao
import com.hyperfetch.dao.TaskChunkDao
import com.hyperfetch.database.AppDatabase
import com.hyperfetch.entity.DownloadTaskEntity
import com.hyperfetch.entity.TaskChunkEntity
import com.hyperfetch.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

/**
 * 任务仓库
 * 提供任务和分块的 CRUD 操作
 */
class TaskRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val taskDao: DownloadTaskDao = database.downloadTaskDao()
    private val chunkDao: TaskChunkDao = database.taskChunkDao()

    /**
     * 获取所有任务
     */
    fun getAllTasks(): Flow<List<DownloadTask>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 获取指定分类的任务
     */
    fun getTasksByCategory(category: Category): Flow<List<DownloadTask>> {
        return taskDao.getTasksByCategory(category.name).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 获取指定状态的任务
     */
    fun getTasksByStatus(status: TaskStatus): Flow<List<DownloadTask>> {
        return taskDao.getTasksByStatus(status.name).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 获取任务（通过ID）
     */
    suspend fun getTaskById(taskId: String): DownloadTask? {
        return taskDao.getTaskById(taskId)?.toModel()
    }

    /**
     * 获取任务（Flow）
     */
    fun getTaskByIdFlow(taskId: String): Flow<DownloadTask?> {
        return taskDao.getTaskByIdFlow(taskId).map { it?.toModel() }
    }

    /**
     * 获取待处理任务（用于恢复）
     */
    suspend fun getPendingTasks(): List<DownloadTask> {
        return taskDao.getPendingTasks().map { it.toModel() }
    }

    /**
     * 获取下载中的任务数
     */
    suspend fun getDownloadingCount(): Int {
        return taskDao.getDownloadingCount()
    }

    /**
     * 获取排队中的任务数
     */
    suspend fun getQueuedCount(): Int {
        return taskDao.getQueuedCount()
    }

    /**
     * 创建任务
     */
    suspend fun createTask(
        url: String,
        options: DownloadOptions,
        savePath: String
    ): DownloadTask {
        val fileName = CategoryService.extractFileName(url)
        val category = options.category ?: CategoryService.classify(url)

        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            url = url,
            fileName = fileName,
            category = category,
            priority = options.priority,
            threadCount = options.threadCount,
            speedLimit = options.speedLimit,
            savePath = savePath,
            status = if (options.startNow) TaskStatus.QUEUED else TaskStatus.PAUSED
        )

        taskDao.insertTask(task.toEntity())
        return task
    }

    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        taskDao.updateTaskStatus(taskId, status.name)
    }

    /**
     * 更新下载进度
     */
    suspend fun updateProgress(taskId: String, downloaded: Long, totalSize: Long) {
        taskDao.updateProgress(taskId, downloaded, totalSize)
    }

    /**
     * 完成任务
     */
    suspend fun completeTask(taskId: String, downloaded: Long) {
        taskDao.completeTask(taskId, TaskStatus.COMPLETED.name, downloaded)
    }

    /**
     * 更新任务错误
     */
    suspend fun updateTaskError(taskId: String, status: TaskStatus, errorMessage: String?) {
        taskDao.updateTaskError(taskId, status.name, errorMessage)
    }

    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String) {
        chunkDao.deleteChunksByTaskId(taskId)
        taskDao.deleteTaskById(taskId)
    }

    /**
     * 删除任务及其文件
     */
    suspend fun deleteTaskAndFile(taskId: String) {
        val task = taskDao.getTaskById(taskId)
        task?.let {
            // 删除文件
            if (it.savePath.isNotEmpty()) {
                java.io.File(it.savePath).delete()
            }
            // 删除数据库记录
            chunkDao.deleteChunksByTaskId(taskId)
            taskDao.deleteTaskById(taskId)
        }
    }

    /**
     * 获取分块
     */
    suspend fun getChunks(taskId: String): List<TaskChunk> {
        return chunkDao.getChunksByTaskId(taskId).map { it.toModel() }
    }

    /**
     * 保存分块
     */
    suspend fun saveChunks(taskId: String, chunks: List<TaskChunk>) {
        chunkDao.insertChunks(chunks.map { it.toEntity() })
    }

    /**
     * 更新分块进度
     */
    suspend fun updateChunkProgress(taskId: String, index: Int, downloaded: Long, status: ChunkStatus) {
        chunkDao.updateChunkProgress(taskId, index, downloaded, status.name)
    }

    /**
     * 删除已完成分块
     */
    suspend fun deleteCompletedChunks(taskId: String) {
        chunkDao.deleteCompletedChunks(taskId)
    }

    /**
     * 转换实体到模型
     */
    private fun DownloadTaskEntity.toModel(): DownloadTask {
        return DownloadTask(
            id = id,
            url = url,
            fileName = fileName,
            totalSize = totalSize,
            downloaded = downloaded,
            category = Category.valueOf(category),
            status = TaskStatus.valueOf(status),
            priority = Priority.valueOf(priority),
            threadCount = threadCount,
            speedLimit = speedLimit,
            savePath = savePath,
            createTime = Date(createTime),
            retryCount = retryCount,
            errorMessage = errorMessage
        )
    }

    /**
     * 转换模型到实体
     */
    private fun DownloadTask.toEntity(): DownloadTaskEntity {
        return DownloadTaskEntity(
            id = id,
            url = url,
            fileName = fileName,
            totalSize = totalSize,
            downloaded = downloaded,
            category = category.name,
            status = status.name,
            priority = priority.name,
            threadCount = threadCount,
            speedLimit = speedLimit,
            savePath = savePath,
            createTime = createTime.time,
            retryCount = retryCount,
            errorMessage = errorMessage
        )
    }

    /**
     * 转换分块实体到模型
     */
    private fun TaskChunkEntity.toModel(): TaskChunk {
        return TaskChunk(
            taskId = taskId,
            index = index,
            start = start,
            end = end,
            downloaded = downloaded,
            status = ChunkStatus.valueOf(status)
        )
    }

    /**
     * 转换分块模型到实体
     */
    private fun TaskChunk.toEntity(): TaskChunkEntity {
        return TaskChunkEntity(
            taskId = taskId,
            index = index,
            start = start,
            end = end,
            downloaded = downloaded,
            status = status.name
        )
    }
}
