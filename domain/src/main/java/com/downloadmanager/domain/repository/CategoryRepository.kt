package com.downloadmanager.domain.repository

import com.downloadmanager.common.FileCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getTasksByCategory(category: FileCategory): Flow<List<com.downloadmanager.common.DownloadTask>>
    fun getCategoryCounts(): Flow<Map<FileCategory, Int>>
}