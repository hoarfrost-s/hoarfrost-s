package com.downloadmanager.data.repository

import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FileCategory
import com.downloadmanager.common.TaskStatus
import com.downloadmanager.data.local.TaskDao
import com.downloadmanager.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : CategoryRepository {

    override fun getTasksByCategory(category: FileCategory): Flow<List<DownloadTask>> {
        return taskDao.observeByCategory(category.name).map { entities ->
            entities.map { entity ->
                DownloadTask(
                    id = entity.id,
                    url = entity.url,
                    fileName = entity.fileName,
                    fileSize = entity.fileSize,
                    downloadedBytes = entity.downloadedBytes,
                    category = category,
                    status = try { TaskStatus.valueOf(entity.status) } catch (e: Exception) { TaskStatus.WAITING },
                    threadCount = entity.threadCount,
                    savePath = entity.savePath,
                    createdAt = entity.createdAt,
                    queuePosition = entity.queuePosition,
                    errorMessage = entity.errorMessage
                )
            }
        }
    }

    override fun getCategoryCounts(): Flow<Map<FileCategory, Int>> {
        return taskDao.getCategoryCounts().map { counts ->
            val map = mutableMapOf<FileCategory, Int>()
            FileCategory.entries.forEach { map[it] = 0 }
            counts.forEach { count ->
                try {
                    val cat = FileCategory.valueOf(count.category)
                    map[cat] = count.count
                } catch (e: Exception) { }
            }
            map
        }
    }
}