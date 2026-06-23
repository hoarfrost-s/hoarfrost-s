package com.downloadmanager.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.engine.DownloadProgress
import com.downloadmanager.app.repository.DownloadRepository
import com.downloadmanager.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeTasksJob: Job? = null
    private val progressJobs = mutableMapOf<String, Job>()
    private val lastNotificationUpdate = mutableMapOf<String, Long>()
    private val currentTaskStates = mutableMapOf<String, DownloadTaskEntity>()

    private val notificationUpdateThrottleMs = 1000L

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()
        startObservingActiveTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            NotificationHelper.ACTION_PAUSE -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.pauseDownload(it)
                    }
                }
            }
            NotificationHelper.ACTION_RESUME -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.resumeDownload(it)
                    }
                }
            }
            NotificationHelper.ACTION_CANCEL -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.cancelDownload(it)
                        notificationHelper.cancelNotification(it)
                    }
                }
            }
            NotificationHelper.ACTION_RETRY -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.retryDownload(it)
                    }
                }
            }
            NotificationHelper.ACTION_OPEN -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.openFile(it)
                    }
                }
            }
            NotificationHelper.ACTION_SHARE -> {
                val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
                taskId?.let {
                    serviceScope.launch {
                        downloadRepository.launchShareFile(it)
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        activeTasksJob?.cancel()
        progressJobs.values.forEach { it.cancel() }
        progressJobs.clear()
        currentTaskStates.clear()
        lastNotificationUpdate.clear()
        serviceScope.cancel()
    }

    private fun startObservingActiveTasks() {
        activeTasksJob = downloadRepository.observeAllTasks()
            .onEach { allTasks ->
                val activeTasks = allTasks.filter { task ->
                    task.status == DownloadStatus.DOWNLOADING ||
                    task.status == DownloadStatus.WAITING ||
                    task.status == DownloadStatus.PENDING ||
                    task.status == DownloadStatus.PAUSED
                }
                handleActiveTasks(activeTasks)
            }
            .launchIn(serviceScope)
    }

    private fun handleActiveTasks(activeTasks: List<DownloadTaskEntity>) {
        val currentTaskIds = activeTasks.map { it.id }.toSet()

        progressJobs.keys.toList().forEach { taskId ->
            if (taskId !in currentTaskIds) {
                progressJobs.remove(taskId)?.cancel()
                currentTaskStates.remove(taskId)
                lastNotificationUpdate.remove(taskId)
            }
        }

        activeTasks.forEach { task ->
            currentTaskStates[task.id] = task
            if (task.id !in progressJobs) {
                observeTaskProgress(task.id)
            }
        }

        val hasActiveDownloads = activeTasks.any { task ->
            task.status == DownloadStatus.DOWNLOADING ||
            task.status == DownloadStatus.WAITING ||
            task.status == DownloadStatus.PENDING
        }

        if (activeTasks.isEmpty()) {
            stopSelf()
        } else if (hasActiveDownloads) {
            val firstTask = activeTasks.first { task ->
                task.status == DownloadStatus.DOWNLOADING ||
                task.status == DownloadStatus.WAITING ||
                task.status == DownloadStatus.PENDING
            }
            startForegroundIfNeeded(firstTask)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    private fun observeTaskProgress(taskId: String) {
        val job = downloadRepository.observeDownloadProgress(taskId)
            .onEach { progress ->
                val task = currentTaskStates[taskId] ?: return@onEach
                updateNotificationIfNeeded(task, progress)
            }
            .launchIn(serviceScope)
        progressJobs[taskId] = job
    }

    private fun startForegroundIfNeeded(task: DownloadTaskEntity) {
        serviceScope.launch {
            try {
                val progress = DownloadProgress(
                    taskId = task.id,
                    downloadedSize = task.downloadedSize,
                    totalSize = task.totalSize,
                    speed = 0L,
                    status = task.status
                )
                val notification = notificationHelper.buildDownloadingNotification(task, progress)
                startForeground(task.id.hashCode(), notification.build())
            } catch (e: Exception) {
            }
        }
    }

    private fun updateNotificationIfNeeded(task: DownloadTaskEntity, progress: DownloadProgress) {
        val now = System.currentTimeMillis()
        val lastUpdate = lastNotificationUpdate[task.id] ?: 0L

        if (now - lastUpdate < notificationUpdateThrottleMs &&
            progress.status == DownloadStatus.DOWNLOADING
        ) {
            return
        }

        lastNotificationUpdate[task.id] = now

        val builder = when (progress.status) {
            DownloadStatus.DOWNLOADING, DownloadStatus.WAITING, DownloadStatus.PENDING -> {
                notificationHelper.buildDownloadingNotification(task, progress)
            }
            DownloadStatus.PAUSED -> {
                notificationHelper.buildPausedNotification(task)
            }
            DownloadStatus.COMPLETED -> {
                notificationHelper.buildCompletedNotification(task)
            }
            DownloadStatus.FAILED -> {
                notificationHelper.buildFailedNotification(task)
            }
            else -> null
        }

        builder?.let {
            notificationHelper.updateNotification(task.id, it)
        }
    }
}
