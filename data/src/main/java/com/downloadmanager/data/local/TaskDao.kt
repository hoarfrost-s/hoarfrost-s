package com.downloadmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY created_at ASC")
    fun observeByStatus(status: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE category = :category ORDER BY created_at DESC")
    fun observeByCategory(category: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status IN ('WAITING', 'QUEUED') ORDER BY created_at ASC")
    suspend fun getQueuedTasks(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    suspend fun getById(taskId: String): DownloadTaskEntity?

    @Query("SELECT COUNT(*) FROM download_tasks WHERE category = :category")
    fun countByCategory(category: String): Flow<Int>

    @Query("SELECT category, COUNT(*) as count FROM download_tasks GROUP BY category")
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: String)

    @Query("UPDATE download_tasks SET downloaded_bytes = :downloadedBytes, status = :status WHERE id = :taskId")
    suspend fun updateProgress(taskId: String, downloadedBytes: Long, status: String)

    @Query("DELETE FROM download_tasks WHERE id = :taskId")
    suspend fun delete(taskId: String)

    @Query("DELETE FROM download_tasks WHERE id IN (:taskIds)")
    suspend fun deleteAll(taskIds: List<String>)
}

data class CategoryCount(
    val category: String,
    val count: Int
)