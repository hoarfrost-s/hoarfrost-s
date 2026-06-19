package com.downloadmanager.engine

import com.downloadmanager.common.DownloadProgress
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.TaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class DefaultTaskScheduler(
    private val engine: DownloadEngine,
    maxConcurrency: Int = 3
) : TaskScheduler {
    private val slotManager = SemaphoreSlotManager(maxConcurrency)
    private val waitingChannel = Channel<DownloadTask>(Channel.UNLIMITED)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val schedulerMutex = Mutex()
    private val pausedTasks = mutableSetOf<String>()
    private val _progressFlow = MutableSharedFlow<DownloadProgress>(replay = 0)
    private val waitingTasks = mutableListOf<DownloadTask>()

    override suspend fun submit(task: DownloadTask) {
        schedulerMutex.withLock {
            if (task.status == TaskStatus.PAUSED) {
                // Resuming a paused task
                if (slotManager.tryAcquire(task.id)) {
                    startDownload(task)
                } else {
                    task.status.let { }
                    waitingChannel.send(task)
                }
            } else if (slotManager.tryAcquire(task.id)) {
                startDownload(task)
            } else {
                val queuedTask = task.copy(status = TaskStatus.QUEUED)
                waitingChannel.send(queuedTask)
            }
        }
    }

    override fun setMaxConcurrency(max: Int) {
        slotManager.resize(max)
    }

    override fun getActiveCount(): Int = activeJobs.size

    override fun getQueuedCount(): Int = waitingTasks.size

    override suspend fun pauseTask(taskId: String) {
        pausedTasks.add(taskId)
        engine.pause(taskId)
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        slotManager.release(taskId)
        drainQueue()
    }

    override suspend fun resumeTask(taskId: String) {
        pausedTasks.remove(taskId)
        engine.resume(taskId)
        // Will be resubmitted
    }

    override suspend fun cancelTask(taskId: String) {
        engine.cancel(taskId)
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        slotManager.release(taskId)
        drainQueue()
    }

    override suspend fun pauseAll() {
        activeJobs.keys.toList().forEach { pauseTask(it) }
    }

    override suspend fun resumeAll() {
        pausedTasks.toList().forEach { resumeTask(it) }
    }

    override suspend fun restoreTasks(tasks: List<DownloadTask>) {
        tasks.forEach { task ->
            if (task.status == TaskStatus.WAITING || task.status == TaskStatus.QUEUED) {
                submit(task)
            }
        }
    }

    override fun getProgressFlow(): Flow<DownloadProgress> = _progressFlow.asSharedFlow()

    private fun startDownload(task: DownloadTask) {
        val downloadTask = task.copy(status = TaskStatus.DOWNLOADING)
        val job = scope.launch {
            try {
                engine.execute(downloadTask).collect { progress ->
                    _progressFlow.emit(progress)
                }
                slotManager.release(downloadTask.id)
                activeJobs.remove(downloadTask.id)
                drainQueue()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    slotManager.release(downloadTask.id)
                    activeJobs.remove(downloadTask.id)
                    drainQueue()
                }
            }
        }
        activeJobs[downloadTask.id] = job
    }

    private suspend fun drainQueue() {
        schedulerMutex.withLock {
            while (slotManager.tryAcquire("dummy")) {
                val next = waitingChannel.tryReceive().getOrNull() ?: run {
                    slotManager.release("dummy")
                    break
                }
                startDownload(next)
            }
        }
    }
}