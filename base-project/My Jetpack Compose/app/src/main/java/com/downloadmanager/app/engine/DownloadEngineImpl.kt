package com.downloadmanager.app.engine

import com.downloadmanager.app.data.entity.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngineImpl @Inject constructor(
    private val client: OkHttpClient,
    private val globalTokenBucket: TokenBucket
) : DownloadEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var maxConcurrent: Int = 3

    private val pendingQueue = ArrayDeque<PendingTask>()
    private val activeTasks = ConcurrentHashMap<String, ActiveTask>()
    private val progressFlows = ConcurrentHashMap<String, MutableStateFlow<DownloadProgress>>()
    private val downloaders = ConcurrentHashMap<String, ChunkedDownloader>()

    private val mutex = Mutex()

    private val _progressEvent = MutableSharedFlow<DownloadProgress>(extraBufferCapacity = 100)
    val progressEvent: SharedFlow<DownloadProgress> = _progressEvent.asSharedFlow()

    data class PendingTask(
        val taskId: String,
        val config: DownloadTaskConfig
    )

    data class ActiveTask(
        val taskId: String,
        val config: DownloadTaskConfig,
        val job: Job
    )

    override fun enqueue(config: DownloadTaskConfig): String {
        val taskId = UUID.randomUUID().toString()

        val initialProgress = DownloadProgress(
            taskId = taskId,
            downloadedSize = 0L,
            totalSize = 0L,
            speed = 0L,
            status = DownloadStatus.WAITING
        )
        progressFlows[taskId] = MutableStateFlow(initialProgress)

        scope.launch {
            mutex.withLock {
                pendingQueue.add(PendingTask(taskId, config))
            }
            scheduleNext()
        }

        return taskId
    }

    override fun pause(taskId: String) {
        val downloader = downloaders[taskId]
        if (downloader != null) {
            downloader.pause()
            scope.launch {
                progressFlows[taskId]?.let { flow ->
                    val current = flow.value
                    flow.value = current.copy(status = DownloadStatus.PAUSED)
                }
            }
        }
    }

    override fun resume(taskId: String) {
        val downloader = downloaders[taskId]
        if (downloader != null) {
            downloader.resume()
            scope.launch {
                progressFlows[taskId]?.let { flow ->
                    val current = flow.value
                    flow.value = current.copy(status = DownloadStatus.DOWNLOADING)
                }
            }
        } else {
            scope.launch {
                var found = false
                mutex.withLock {
                    found = pendingQueue.any { it.taskId == taskId }
                }
                if (found) {
                    progressFlows[taskId]?.let { flow ->
                        flow.value = flow.value.copy(status = DownloadStatus.WAITING)
                    }
                }
            }
        }
    }

    override fun cancel(taskId: String, deleteFile: Boolean) {
        scope.launch {
            val activeTask = activeTasks[taskId]
            if (activeTask != null) {
                downloaders[taskId]?.cancel()
                activeTask.job.cancel()
                if (deleteFile) {
                    try {
                        java.io.File(activeTask.config.tempPath).takeIf { it.exists() }?.delete()
                        java.io.File(activeTask.config.savePath).takeIf { it.exists() }?.delete()
                    } catch (_: Exception) {
                    }
                }
                return@launch
            }

            mutex.withLock {
                val iterator = pendingQueue.iterator()
                while (iterator.hasNext()) {
                    val task = iterator.next()
                    if (task.taskId == taskId) {
                        iterator.remove()
                        if (deleteFile) {
                            try {
                                java.io.File(task.config.tempPath).takeIf { it.exists() }?.delete()
                                java.io.File(task.config.savePath).takeIf { it.exists() }?.delete()
                            } catch (_: Exception) {
                            }
                        }
                        break
                    }
                }
            }

            progressFlows[taskId]?.let { flow ->
                flow.value = flow.value.copy(
                    status = DownloadStatus.CANCELLED,
                    errorMessage = null
                )
            }
        }
    }

    override fun observeProgress(taskId: String): Flow<DownloadProgress> {
        val flow = progressFlows.getOrPut(taskId) {
            MutableStateFlow(
                DownloadProgress(
                    taskId = taskId,
                    downloadedSize = 0L,
                    totalSize = 0L,
                    speed = 0L,
                    status = DownloadStatus.PENDING
                )
            )
        }
        return flow.asStateFlow()
    }

    override fun setGlobalSpeedLimit(bytesPerSecond: Long) {
        globalTokenBucket.setRate(bytesPerSecond)
    }

    override fun setMaxConcurrentTasks(maxConcurrent: Int) {
        require(maxConcurrent > 0) { "maxConcurrent must be > 0" }
        this.maxConcurrent = maxConcurrent
        scope.launch {
            scheduleNext()
        }
    }

    override fun getActiveTaskCount(): Int = activeTasks.size

    override fun getPendingTaskCount(): Int = pendingQueue.size

    private suspend fun scheduleNext() {
        while (activeTasks.size < maxConcurrent && pendingQueue.isNotEmpty()) {
            val pendingTask: PendingTask? = mutex.withLock {
                if (pendingQueue.isNotEmpty()) pendingQueue.removeFirstOrNull() else null
            }
            if (pendingTask != null) {
                startTask(pendingTask)
            } else {
                break
            }
        }
    }

    private fun startTask(pendingTask: PendingTask) {
        val taskId = pendingTask.taskId
        val config = pendingTask.config

        val downloader = ChunkedDownloader(client, globalTokenBucket)
        downloaders[taskId] = downloader

        val job = scope.launch {
            try {
                progressFlows[taskId]?.let { flow ->
                    val current = flow.value
                    flow.value = current.copy(status = DownloadStatus.DOWNLOADING)
                }

                val result = downloader.download(taskId, config) { progress ->
                    scope.launch {
                        progressFlows[taskId]?.value = progress
                        _progressEvent.tryEmit(progress)
                    }
                }

                when (result) {
                    is DownloadResult.Success -> {
                        progressFlows[taskId]?.let { flow ->
                            flow.value = flow.value.copy(
                                status = DownloadStatus.COMPLETED,
                                speed = 0L
                            )
                        }
                    }
                    is DownloadResult.Paused -> {
                        progressFlows[taskId]?.let { flow ->
                            flow.value = flow.value.copy(
                                status = DownloadStatus.PAUSED,
                                speed = 0L
                            )
                        }
                    }
                    is DownloadResult.Cancelled -> {
                        progressFlows[taskId]?.let { flow ->
                            flow.value = flow.value.copy(
                                status = DownloadStatus.CANCELLED,
                                speed = 0L
                            )
                        }
                    }
                    is DownloadResult.Error -> {
                        progressFlows[taskId]?.let { flow ->
                            flow.value = flow.value.copy(
                                status = DownloadStatus.FAILED,
                                speed = 0L,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                progressFlows[taskId]?.let { flow ->
                    flow.value = flow.value.copy(
                        status = DownloadStatus.FAILED,
                        speed = 0L,
                        errorMessage = e.message
                    )
                }
            } finally {
                activeTasks.remove(taskId)
                downloaders.remove(taskId)
                scheduleNext()
            }
        }

        activeTasks[taskId] = ActiveTask(taskId, config, job)
    }

    fun destroy() {
        scope.cancel()
    }
}
