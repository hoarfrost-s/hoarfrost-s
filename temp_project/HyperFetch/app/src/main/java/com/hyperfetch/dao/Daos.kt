package com.hyperfetch.dao

import androidx.room.*
import com.hyperfetch.entity.DownloadTaskEntity
import com.hyperfetch.entity.TaskChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 下载任务 DAO
 */
@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY createTime DESC")
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE category = :category ORDER BY createTime DESC")
    fun getTasksByCategory(category: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY createTime DESC")
    fun getTasksByStatus(status: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    suspend fun getPendingTasks(): List<DownloadTaskEntity>

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'DOWNLOADING'")
    suspend fun getDownloadingCount(): Int

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'QUEUED'")
    suspend fun getQueuedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String)

    @Query("UPDATE download_tasks SET downloaded = :downloaded, totalSize = :totalSize WHERE id = :taskId")
    suspend fun updateProgress(taskId: String, downloaded: Long, totalSize: Long)

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :taskId")
    suspend fun updateTaskError(taskId: String, status: String, errorMessage: String?)

    @Query("UPDATE download_tasks SET status = :status, downloaded = :downloaded, errorMessage = null WHERE id = :taskId")
    suspend fun completeTask(taskId: String, status: String, downloaded: Long)

    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM download_tasks")
    suspend fun deleteAllTasks()
}

/**
 * 任务分块 DAO
 */
@Dao
interface TaskChunkDao {

    @Query("SELECT * FROM task_chunks WHERE taskId = :taskId ORDER BY `index` ASC")
    suspend fun getChunksByTaskId(taskId: String): List<TaskChunkEntity>

    @Query("SELECT * FROM task_chunks WHERE taskId = :taskId ORDER BY `index` ASC")
    fun getChunksByTaskIdFlow(taskId: String): Flow<List<TaskChunkEntity>>

    @Query("SELECT * FROM task_chunks WHERE taskId = :taskId AND `index` = :index")
    suspend fun getChunk(taskId: String, index: Int): TaskChunkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: TaskChunkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<TaskChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: TaskChunkEntity)

    @Query("UPDATE task_chunks SET downloaded = :downloaded, status = :status WHERE taskId = :taskId AND `index` = :index")
    suspend fun updateChunkProgress(taskId: String, index: Int, downloaded: Long, status: String)

    @Query("DELETE FROM task_chunks WHERE taskId = :taskId")
    suspend fun deleteChunksByTaskId(taskId: String)

    @Query("DELETE FROM task_chunks WHERE taskId = :taskId AND status = 'COMPLETED'")
    suspend fun deleteCompletedChunks(taskId: String)
}
