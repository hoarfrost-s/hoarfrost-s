package com.downloadmanager.domain.usecase

import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FileCategory
import com.downloadmanager.common.LinkInfo
import com.downloadmanager.common.UrlParser
import com.downloadmanager.domain.repository.ConfigRepository
import com.downloadmanager.domain.repository.TaskRepository
import java.util.UUID

class AddTaskUseCase(
    private val taskRepository: TaskRepository,
    private val configRepository: ConfigRepository
) {
    suspend fun invoke(url: String): DownloadTask {
        val fileName = UrlParser.extractFileName(url)
        val extension = UrlParser.extractExtension(url)
        val category = FileCategory.fromExtension(extension)

        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = taskId,
            url = url,
            fileName = fileName,
            category = category,
            linkInfo = LinkInfo(
                url = url,
                fileName = fileName,
                fileSize = -1,
                contentType = null,
                category = category,
                threadCount = 4,
                savePath = "",
                createdAt = System.currentTimeMillis()
            )
        )
        taskRepository.insertTask(task)
        return task
    }
}