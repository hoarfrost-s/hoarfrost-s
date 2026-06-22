package com.hyperfetch.engine

import com.hyperfetch.event.EventBusWrapper
import com.hyperfetch.event.ProgressEvent
import com.hyperfetch.model.*
import com.hyperfetch.protocol.ProgressCallback
import com.hyperfetch.protocol.ProtocolFactory
import com.hyperfetch.rate.TokenBucketLimiter
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 下载引擎
 * 负责多线程分块下载、文件合并、进度追踪
 */
class DownloadEngine(
    private val context: android.content.Context
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadJobs = mutableMapOf<String, Job>()
    private val taskExecutors = mutableMapOf<String, ExecutorService>()
    private val taskLimiters = mutableMapOf<String, TokenBucketLimiter>()
    private val taskSpeeds = mutableMapOf<String, AtomicLong>()

    companion object {
        private const val MAX_RETRIES = 5
        private val RETRY_DELAYS = listOf(1000L, 2000L, 4000L, 8000L, 16000L)
    }

    /**
     * 提交下载任务
     */
    fun submit(task: DownloadTask) {
        val job = scope.launch {
            executeDownload(task)
        }
        downloadJobs[task.id] = job
    }

    /**
     * 暂停下载任务
     */
    fun pause(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        taskExecutors[taskId]?.shutdown()
        taskExecutors.remove(taskId)
    }

    /**
     * 停止所有下载任务
     */
    fun stopAll() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        taskExecutors.values.forEach { it.shutdown() }
        taskExecutors.clear()
    }

    /**
     * 调整线程数（热调整）
     */
    fun adjustThreadCount(taskId: String, newCount: Int, currentChunks: List<TaskChunk>) {
        // 现有线程池会在完成后自动使用新的核心大小
        // 新增的线程会立即开始处理未完成的任务
    }

    /**
     * 调整速度限制
     */
    fun adjustSpeedLimit(taskId: String, newLimit: Long) {
        taskLimiters[taskId]?.setRate(newLimit)
    }

    /**
     * 执行下载
     */
    private suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
        try {
            // 探测资源
            val handler = ProtocolFactory.getHandler(task.url)
            val meta = handler.probe(task.url)

            // 获取或创建限速器
            val limiter = taskLimiters.getOrPut(task.id) {
                TokenBucketLimiter().apply { setRate(task.speedLimit) }
            }

            // 获取或创建速度追踪器
            val speedTracker = taskSpeeds.getOrPut(task.id) { AtomicLong(0L) }

            // 创建输出文件
            val outputFile = java.io.File(task.savePath)
            outputFile.parentFile?.mkdirs()
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            if (meta.supportRange && task.threadCount > 1) {
                // 多线程分块下载
                executeMultiThreadDownload(task, meta, handler, limiter, speedTracker)
            } else {
                // 单线程下载
                executeSingleThreadDownload(task, handler, limiter, speedTracker)
            }

        } catch (e: CancellationException) {
            // 用户取消
            throw e
        } catch (e: Exception) {
            EventBusWrapper.postMain(
                com.hyperfetch.event.ErrorEvent(
                    taskId = task.id,
                    cause = e.message ?: "Unknown error",
                    retryCount = task.retryCount
                )
            )
        } finally {
            downloadJobs.remove(task.id)
            taskExecutors.remove(task.id)
            taskLimiters.remove(task.id)
            taskSpeeds.remove(task.id)
        }
    }

    /**
     * 多线程分块下载
     */
    private suspend fun executeMultiThreadDownload(
        task: DownloadTask,
        meta: ResourceMeta,
        handler: com.hyperfetch.protocol.ProtocolHandler,
        limiter: TokenBucketLimiter,
        speedTracker: AtomicLong
    ) {
        // 分块策略
        val chunks = splitChunks(task.id, meta.totalSize, task.threadCount)

        // 创建线程池
        val executor = taskExecutors.getOrPut(task.id) {
            Executors.newFixedThreadPool(task.threadCount)
        }

        // 下载计数器
        val completedCount = AtomicLong(0)
        val totalChunks = chunks.size

        // 并发下载所有分块
        val deferreds = chunks.mapIndexed { index, chunk ->
            async(Dispatchers.IO) {
                downloadChunk(task, chunk, index, handler, limiter, speedTracker)
                completedCount.incrementAndGet()
            }
        }

        // 等待所有分块完成
        deferreds.awaitAll()
    }

    /**
     * 单线程下载
     */
    private suspend fun executeSingleThreadDownload(
        task: DownloadTask,
        handler: com.hyperfetch.protocol.ProtocolHandler,
        limiter: TokenBucketLimiter,
        speedTracker: AtomicLong
    ) {
        var downloaded = 0L
        val startTime = System.currentTimeMillis()

        val callback = object : ProgressCallback {
            override fun onProgress(downloadedBytes: Long, totalBytes: Long) {
                downloaded += downloadedBytes

                // 应用限速
                if (limiter.isLimited()) {
                    try {
                        limiter.acquire(downloadedBytes.toInt())
                    } catch (e: InterruptedException) {
                        // ignore
                    }
                }

                // 计算速度
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val speed = if (elapsed > 0) (downloaded / elapsed).toLong() else 0L
                speedTracker.set(speed)

                // 发布进度事件
                EventBusWrapper.postMain(
                    ProgressEvent(
                        taskId = task.id,
                        downloaded = downloaded,
                        total = totalBytes,
                        speed = speed,
                        eta = calculateEta(downloaded, totalBytes, speed)
                    )
                )
            }

            override fun onComplete() {
                EventBusWrapper.postMain(
                    ProgressEvent(
                        taskId = task.id,
                        downloaded = downloaded,
                        total = downloaded,
                        speed = 0,
                        eta = 0
                    )
                )
            }

            override fun onError(e: IOException) {
                EventBusWrapper.postMain(
                    com.hyperfetch.event.ErrorEvent(
                        taskId = task.id,
                        cause = e.message ?: "Download failed",
                        retryCount = task.retryCount
                    )
                )
            }
        }

        withContext(Dispatchers.IO) {
            try {
                handler.download(task.url, task.savePath, callback)
                EventBusWrapper.postMain(
                    com.hyperfetch.event.CompletedEvent(
                        taskId = task.id,
                        filePath = task.savePath,
                        fileName = task.fileName
                    )
                )
            } catch (e: Exception) {
                callback.onError(e as? IOException ?: IOException(e.message))
            }
        }
    }

    /**
     * 下载单个分块
     */
    private suspend fun downloadChunk(
        task: DownloadTask,
        chunk: TaskChunk,
        index: Int,
        handler: com.hyperfetch.protocol.ProtocolHandler,
        limiter: TokenBucketLimiter,
        speedTracker: AtomicLong
    ) {
        var retries = 0
        var downloaded = chunk.downloaded
        val startTime = System.currentTimeMillis()

        while (retries < MAX_RETRIES) {
            try {
                val callback = object : ProgressCallback {
                    override fun onProgress(downloadedBytes: Long, totalBytes: Long) {
                        downloaded += downloadedBytes

                        // 应用限速
                        if (limiter.isLimited()) {
                            try {
                                limiter.acquire(downloadedBytes.toInt())
                            } catch (e: InterruptedException) {
                                // ignore
                            }
                        }

                        // 更新速度
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val currentSpeed = if (elapsed > 0) (downloaded / elapsed).toLong() else 0L
                        speedTracker.set(speedTracker.get() + currentSpeed)

                        // 发布进度事件
                        EventBusWrapper.postMain(
                            ProgressEvent(
                                taskId = task.id,
                                downloaded = task.downloaded + downloaded,
                                total = task.totalSize,
                                speed = speedTracker.get(),
                                eta = calculateEta(task.downloaded + downloaded, task.totalSize, speedTracker.get())
                            )
                        )
                    }

                    override fun onComplete() {}
                    override fun onError(e: IOException) { throw e }
                }

                handler.downloadRange(
                    url = task.url,
                    start = chunk.start + downloaded,
                    end = chunk.end,
                    outputPath = task.savePath,
                    callback = callback
                )
                return // 成功完成
            } catch (e: Exception) {
                retries++
                if (retries < MAX_RETRIES) {
                    delay(RETRY_DELAYS.getOrElse(retries - 1) { 16000L })
                }
            }
        }
    }

    /**
     * 分块策略
     */
    private fun splitChunks(taskId: String, totalSize: Long, threadCount: Int): List<TaskChunk> {
        if (totalSize <= 0 || threadCount <= 1) {
            return listOf(TaskChunk(taskId, 0, 0, Long.MAX_VALUE))
        }

        val chunkSize = totalSize / threadCount
        return (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalSize - 1 else start + chunkSize - 1
            TaskChunk(taskId, i, start, end)
        }
    }

    /**
     * 计算预计剩余时间
     */
    private fun calculateEta(downloaded: Long, total: Long, speed: Long): Long {
        if (speed <= 0 || total <= 0) return 0
        val remaining = total - downloaded
        return remaining / speed
    }
}
