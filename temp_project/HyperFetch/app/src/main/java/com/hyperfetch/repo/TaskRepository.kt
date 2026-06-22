package com.hyperfetch.repo

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hyperfetch.classify.CategoryService
import com.hyperfetch.database.Download_tasks
import com.hyperfetch.database.HyperFetchDatabase
import com.hyperfetch.database.Task_chunks
import com.hyperfetch.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

/**
 * 任务仓库
 * 提供任务和分块的 CRUD 操作
 */
class TaskRepository(context: Context) {

    private val database = HyperFetchDatabase(
        driver = app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            schema = HyperFetchDatabase.Schema,
            context = context,
            name = "hyperfetch_db"
        )
    )

    /**
     * 获取所有任务
     */
    fun getAllTasks(): Flow<List<DownloadTask>> {
        return database.download_tasksQueries.selectAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toModel() } }
    }

    /**
     * 获取指定分类的任务
     */
    fun getTasksByCategory(category: Category): Flow<List<DownloadTask>> {
        return database.download_tasksQueries.selectTasksByCategory(category.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toModel() } }
    }

    /**
     * 获取指定状态的任务
     */
    fun getTasksByStatus(status: TaskStatus): Flow<List<DownloadTask>> {
        return database.download_tasksQueries.selectTasksByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toModel() } }
    }

    /**
     * 获取任务（通过ID）
     */
    suspend fun getTaskById(taskId: String): DownloadTask? {
        return withContext(Dispatchers.IO) {
            database.download_tasksQueries.selectTaskById(taskId)
                .executeAsOneOrNull()
                ?.toModel()
        }
    }

    /**
     * 获取任务（Flow）
     */
    fun getTaskByIdFlow(taskId: String): Flow<DownloadTask?> {
        return database.download_tasksQueries.selectTaskById(taskId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toModel() }
    }

    /**
     * 获取待处理任务（用于恢复）
     */
    suspend fun getPendingTasks(): List<DownloadTask> {
        return withContext(Dispatchers.IO) {
            database.download_tasksQueries.selectPendingTasks()
                .executeAsList()
                .map { it.toModel() }
        }
    }

    /**
     * 获取下载中的任务数
     */
    suspend fun getDownloadingCount(): Int {
        return withContext(Dispatchers.IO) {
            database.download_tasksQueries.selectDownloadingCount()
                .executeAsOne().toInt()
        }
    }

    /**
     * 获取排队中的任务数
     */
    suspend fun getQueuedCount(): Int {
        return withContext(Dispatchers.IO) {
            database.download_tasksQueries.selectQueuedCount()
                .executeAsOne().toInt()
        }
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

        withContext(Dispatchers.IO) {
            database.download_tasksQueries.insertTask(task.toEntity())
        }
        return task
    }

    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        withContext(Dispatchers.IO) {
            database.download_tasksQueries.updateTaskStatus(status.name, taskId)
        }
    }

    /**
     * 更新下载进度
     */
    suspend fun updateProgress(taskId: String, downloaded: Long, totalSize: Long) {
        withContext(Dispatchers.IO) {
            database.download_tasksQueries.updateProgress(downloaded, totalSize, taskId)
        }
    }

    /**
     * 完成任务
     */
    suspend fun completeTask(taskId: String, downloaded: Long) {
        withContext(Dispatchers.IO) {
            database.download_tasksQueries.completeTask(TaskStatus.COMPLETED.name, downloaded, taskId)
        }
    }

    /**
     * 更新任务错误
     */
    suspend fun updateTaskError(taskId: String, status: TaskStatus, errorMessage: String?) {
        withContext(Dispatchers.IO) {
            database.download_tasksQueries.updateTaskError(status.name, errorMessage, taskId)
        }
    }

    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String) {
        withContext(Dispatchers.IO) {
            database.task_chunksQueries.deleteChunksByTaskId(taskId)
            database.download_tasksQueries.deleteTaskById(taskId)
        }
    }

    /**
     * 删除任务及其文件
     */
    suspend fun deleteTaskAndFile(taskId: String) {
        withContext(Dispatchers.IO) {
            val task = database.download_tasksQueries.selectTaskById(taskId)
                .executeAsOneOrNull()

            task?.let {
                if (it.savePath.isNotEmpty()) {
                    java.io.File(it.savePath).delete()
                }
                database.task_chunksQueries.deleteChunksByTaskId(taskId)
                database.download_tasksQueries.deleteTaskById(taskId)
            }
        }
    }

    /**
     * 获取分块
     */
    suspend fun getChunks(taskId: String): List<TaskChunk> {
        return withContext(Dispatchers.IO) {
            database.task_chunksQueries.selectChunksByTaskId(taskId)
                .executeAsList()
                .map { it.toModel() }
        }
    }

    /**
     * 保存分块
     */
    suspend fun saveChunks(taskId: String, chunks: List<TaskChunk>) {
        withContext(Dispatchers.IO) {
            database.transaction {
                chunks.forEach { chunk ->
                    database.task_chunksQueries.insertChunk(chunk.toEntity())
                }
            }
        }
    }

    /**
     * 更新分块进度
     */
    suspend fun updateChunkProgress(taskId: String, index: Int, downloaded: Long, status: ChunkStatus) {
        withContext(Dispatchers.IO) {
            database.task_chunksQueries.updateChunk(downloaded, status.name, taskId, index.toLong())
        }
    }

    /**
     * 删除已完成分块
     */
    suspend fun deleteCompletedChunks(taskId: String) {
        withContext(Dispatchers.IO) {
            database.task_chunksQueries.deleteCompletedChunks(taskId)
        }
    }

    // ============ 实体转换方法 ============

    private fun Download_tasks.toModel(): DownloadTask {
        return DownloadTask(
            id = id,
            url = url,
            fileName = fileName,
            totalSize = totalSize,
            downloaded = downloaded,
            category = Category.valueOf(category),
            status = TaskStatus.valueOf(status),
            priority = Priority.valueOf(priority),
            threadCount = threadCount.toInt(),
            speedLimit = speedLimit,
            savePath = savePath,
            createTime = Date(createTime),
            retryCount = retryCount.toInt(),
            errorMessage = errorMessage,
            eta = 0L,
            speed = 0L
        )
    }

    private fun DownloadTask.toEntity(): Download_tasks {
        return Download_tasks(
            id = id,
            url = url,
            fileName = fileName,
            totalSize = totalSize,
            downloaded = downloaded,
            category = category.name,
            status = status.name,
            priority = priority.name,
            threadCount = threadCount.toLong(),
            speedLimit = speedLimit,
            savePath = savePath,
            createTime = createTime.time,
            retryCount = retryCount.toLong(),
            errorMessage = errorMessage
        )
    }

    private fun Task_chunks.toModel(): TaskChunk {
        return TaskChunk(
            taskId = taskId,
            index = index.toInt(),
            start = start,
            end = end,
            downloaded = downloaded,
            status = ChunkStatus.valueOf(status)
        )
    }

    private fun TaskChunk.toEntity(): Task_chunks {
        return Task_chunks(
            taskId = taskId,
            index = index.toLong(),
            start = start,
            end = end,
            downloaded = downloaded,
            status = status.name
        )
    }
}
