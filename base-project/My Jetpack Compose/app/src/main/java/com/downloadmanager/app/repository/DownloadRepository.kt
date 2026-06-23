package com.downloadmanager.app.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.data.local.DownloadDao
import com.downloadmanager.app.engine.DownloadEngine
import com.downloadmanager.app.engine.DownloadProgress
import com.downloadmanager.app.engine.DownloadTaskConfig
import com.downloadmanager.app.encrypt.FileEncryptor
import com.downloadmanager.app.service.DownloadService
import com.downloadmanager.app.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val downloadEngine: DownloadEngine,
    private val fileEncryptor: FileEncryptor,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun addDownload(url: String, fileName: String? = null): String? =
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settingsFlow.first()
                val actualFileName = fileName ?: FileUtils.getFileNameFromUrl(url)
                val originalName = actualFileName

                val matchResult = categoryRepository.matchCategory(actualFileName)

                val categoryPath = matchResult.categoryName
                val subCategoryPath = if (matchResult.extension.isNotEmpty()) {
                    matchResult.extension.uppercase()
                } else {
                    "其他"
                }

                val saveDir = "${settings.downloadRootPath}/$categoryPath/$subCategoryPath"
                fileRepository.ensureDirectory(saveDir)

                val finalFileName = if (settings.enableEncryption) {
                    fileEncryptor.obfuscateFileName(actualFileName)
                } else {
                    actualFileName
                }

                val savePath = "$saveDir/$finalFileName"
                val tempPath = "$savePath.tmp"

                val config = DownloadTaskConfig(
                    url = url,
                    fileName = finalFileName,
                    savePath = savePath,
                    tempPath = tempPath,
                    threadCount = settings.threadCount,
                    speedLimit = 0L
                )

                val taskId = downloadEngine.enqueue(config)
                val now = System.currentTimeMillis()

                val task = DownloadTaskEntity(
                    id = taskId,
                    url = url,
                    fileName = finalFileName,
                    originalName = originalName,
                    totalSize = 0L,
                    downloadedSize = 0L,
                    threadCount = settings.threadCount,
                    speedLimit = 0L,
                    status = DownloadStatus.WAITING,
                    categoryId = matchResult.categoryId,
                    subCategoryId = matchResult.subId,
                    savePath = savePath,
                    tempPath = tempPath,
                    mimeType = null,
                    md5Hash = null,
                    errorMessage = null,
                    createdAt = now,
                    updatedAt = now,
                    completedAt = null
                )

                downloadDao.insertTask(task)

                observeAndSyncProgress(taskId)
                startDownloadService()

                taskId
            } catch (e: Exception) {
                null
            }
        }

    suspend fun pauseDownload(taskId: String) = withContext(Dispatchers.IO) {
        try {
            downloadEngine.pause(taskId)
            downloadDao.updateTaskStatus(taskId, DownloadStatus.PAUSED, System.currentTimeMillis())
        } catch (e: Exception) {
        }
    }

    suspend fun resumeDownload(taskId: String) = withContext(Dispatchers.IO) {
        try {
            downloadEngine.resume(taskId)
            downloadDao.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING, System.currentTimeMillis())
            startDownloadService()
        } catch (e: Exception) {
        }
    }

    suspend fun cancelDownload(taskId: String, deleteFile: Boolean = true) =
        withContext(Dispatchers.IO) {
            try {
                downloadEngine.cancel(taskId, deleteFile)
                if (deleteFile) {
                    val task = downloadDao.getTaskById(taskId).first()
                    task?.let {
                        fileRepository.deleteFile(it.tempPath)
                        fileRepository.deleteFile(it.savePath)
                    }
                }
                downloadDao.updateTaskStatus(
                    taskId,
                    DownloadStatus.CANCELLED,
                    System.currentTimeMillis()
                )
            } catch (e: Exception) {
            }
        }

    suspend fun pauseAll() = withContext(Dispatchers.IO) {
        try {
            val activeTasks = downloadDao.getActiveTasks().first()
            activeTasks.forEach { task ->
                downloadEngine.pause(task.id)
                downloadDao.updateTaskStatus(task.id, DownloadStatus.PAUSED, System.currentTimeMillis())
            }
        } catch (e: Exception) {
        }
    }

    suspend fun resumeAll() = withContext(Dispatchers.IO) {
        try {
            val pausedTasks = downloadDao.getTasksByStatus(DownloadStatus.PAUSED).first()
            pausedTasks.forEach { task ->
                downloadEngine.resume(task.id)
                downloadDao.updateTaskStatus(task.id, DownloadStatus.DOWNLOADING, System.currentTimeMillis())
            }
            if (pausedTasks.isNotEmpty()) {
                startDownloadService()
            }
        } catch (e: Exception) {
        }
    }

    suspend fun cancelAll(deleteFile: Boolean = true) = withContext(Dispatchers.IO) {
        try {
            val activeTasks = downloadDao.getActiveTasks().first()
            activeTasks.forEach { task ->
                downloadEngine.cancel(task.id, deleteFile)
                if (deleteFile) {
                    fileRepository.deleteFile(task.tempPath)
                    fileRepository.deleteFile(task.savePath)
                }
                downloadDao.updateTaskStatus(
                    task.id,
                    DownloadStatus.CANCELLED,
                    System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
        }
    }

    suspend fun retryDownload(taskId: String): String? = withContext(Dispatchers.IO) {
        try {
            val task = downloadDao.getTaskById(taskId).first() ?: return@withContext null
            val settings = settingsRepository.settingsFlow.first()

            fileRepository.deleteFile(task.tempPath)

            val config = DownloadTaskConfig(
                url = task.url,
                fileName = task.fileName,
                savePath = task.savePath,
                tempPath = task.tempPath,
                threadCount = settings.threadCount,
                speedLimit = 0L
            )

            val newTaskId = downloadEngine.enqueue(config)
            val now = System.currentTimeMillis()

            val newTask = task.copy(
                id = newTaskId,
                status = DownloadStatus.WAITING,
                downloadedSize = 0L,
                totalSize = 0L,
                errorMessage = null,
                createdAt = now,
                updatedAt = now,
                completedAt = null
            )

            downloadDao.insertTask(newTask)

            observeAndSyncProgress(newTaskId)
            startDownloadService()

            newTaskId
        } catch (e: Exception) {
            null
        }
    }

    fun observeTask(taskId: String): Flow<DownloadTaskEntity?> {
        return downloadDao.getTaskById(taskId)
    }

    fun observeAllTasks(): Flow<List<DownloadTaskEntity>> {
        return downloadDao.getAllTasks()
    }

    fun observeTasksByStatus(status: DownloadStatus): Flow<List<DownloadTaskEntity>> {
        return downloadDao.getTasksByStatus(status)
    }

    fun observeRecentTasks(limit: Int): Flow<List<DownloadTaskEntity>> {
        return downloadDao.getRecentTasks(limit)
    }

    suspend fun getTask(taskId: String): DownloadTaskEntity? = withContext(Dispatchers.IO) {
        downloadDao.getTaskById(taskId).first()
    }

    suspend fun getTotalDownloadedSize(): Long = withContext(Dispatchers.IO) {
        downloadDao.getTotalDownloadedSize().first()
    }

    suspend fun getTodayDownloadCount(): Int = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        downloadDao.getTodayDownloadCount(startOfDay, endOfDay).first().toInt()
    }

    suspend fun getDownloadCountByCategory(): Map<String, Int> = withContext(Dispatchers.IO) {
        downloadDao.getDownloadCountByCategory().first().mapValues { it.value.toInt() }
    }

    fun observeDownloadProgress(taskId: String): Flow<DownloadProgress> {
        return downloadEngine.observeProgress(taskId)
    }

    private fun observeAndSyncProgress(taskId: String) {
        scope.launch {
            try {
                downloadEngine.observeProgress(taskId).collect { progress ->
                    val now = System.currentTimeMillis()
                    when (progress.status) {
                        DownloadStatus.DOWNLOADING -> {
                            downloadDao.updateProgress(taskId, progress.downloadedSize, now)
                            downloadDao.updateTaskStatus(taskId, progress.status, now)
                        }
                        DownloadStatus.COMPLETED -> {
                            downloadDao.updateProgress(taskId, progress.downloadedSize, now)
                            downloadDao.updateTaskStatus(taskId, progress.status, now)
                            handleDownloadComplete(taskId)
                        }
                        DownloadStatus.FAILED -> {
                            downloadDao.updateTaskStatus(taskId, progress.status, now)
                        }
                        else -> {
                            downloadDao.updateTaskStatus(taskId, progress.status, now)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun handleDownloadComplete(taskId: String) {
        try {
            val task = downloadDao.getTaskById(taskId).first() ?: return
            val settings = settingsRepository.settingsFlow.first()

            if (settings.enableEncryption) {
                val tempFile = File(task.tempPath)
                val encryptedFile = File(task.savePath)
                if (tempFile.exists()) {
                    fileEncryptor.encrypt(tempFile, encryptedFile)
                    tempFile.delete()
                }
            }

            val now = System.currentTimeMillis()
            val updatedTask = task.copy(
                status = DownloadStatus.COMPLETED,
                completedAt = now,
                updatedAt = now
            )
            downloadDao.updateTask(updatedTask)
        } catch (e: Exception) {
        }
    }

    suspend fun shareFile(taskId: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val task = downloadDao.getTaskById(taskId).first() ?: return@withContext null
            val settings = settingsRepository.settingsFlow.first()

            val fileToShare = if (settings.enableEncryption) {
                val encryptedFile = File(task.savePath)
                val decryptedFile = File(
                    context.cacheDir,
                    "share/${task.originalName}"
                )
                decryptedFile.parentFile?.mkdirs()
                if (fileEncryptor.decrypt(encryptedFile, decryptedFile)) {
                    decryptedFile
                } else {
                    null
                }
            } else {
                File(task.savePath)
            }

            fileToShare?.let { file ->
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun launchShareFile(taskId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = shareFile(taskId) ?: return@withContext false
            val task = downloadDao.getTaskById(taskId).first() ?: return@withContext false

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(task.originalName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "分享文件").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun openFile(taskId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val task = downloadDao.getTaskById(taskId).first() ?: return@withContext false
            val settings = settingsRepository.settingsFlow.first()

            val fileToOpen = if (settings.enableEncryption) {
                val encryptedFile = File(task.savePath)
                val decryptedFile = File(
                    context.cacheDir,
                    "open/${task.originalName}"
                )
                decryptedFile.parentFile?.mkdirs()
                if (fileEncryptor.decrypt(encryptedFile, decryptedFile)) {
                    decryptedFile
                } else {
                    null
                }
            } else {
                File(task.savePath)
            }

            fileToOpen?.let { file ->
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, getMimeType(task.originalName))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteTask(taskId: String, deleteFile: Boolean = true) =
        withContext(Dispatchers.IO) {
            try {
                val task = downloadDao.getTaskById(taskId).first()
                downloadEngine.cancel(taskId, deleteFile)
                if (deleteFile && task != null) {
                    fileRepository.deleteFile(task.tempPath)
                    fileRepository.deleteFile(task.savePath)
                }
                downloadDao.deleteTask(taskId)
            } catch (e: Exception) {
            }
        }

    private fun getMimeType(fileName: String): String {
        val extension = FileUtils.getExtension(fileName)
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            else -> "*/*"
        }
    }

    private fun startDownloadService() {
        try {
            val intent = Intent(context, DownloadService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
        }
    }
}
