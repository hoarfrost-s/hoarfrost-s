package com.downloadmanager.app.engine

import com.downloadmanager.app.data.entity.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class ChunkedDownloader(
    private val client: OkHttpClient,
    private val globalTokenBucket: TokenBucket
) {

    private val isPaused = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    fun cancel() {
        isCancelled.set(true)
        isPaused.set(false)
    }

    fun reset() {
        isPaused.set(false)
        isCancelled.set(false)
    }

    suspend fun download(
        taskId: String,
        config: DownloadTaskConfig,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        reset()
        val tempFile = File(config.tempPath)
        tempFile.parentFile?.mkdirs()

        try {
            val headResult = checkFileInfo(config.url)
            val totalSize = headResult.totalSize
            val supportsRange = headResult.supportsRange

            val existingSize = getExistingDownloadedSize(tempFile)
            val downloadedSize = AtomicLong(existingSize)

            onProgress(
                DownloadProgress(
                    taskId = taskId,
                    downloadedSize = existingSize,
                    totalSize = totalSize,
                    speed = 0L,
                    status = DownloadStatus.DOWNLOADING
                )
            )

            val speedCalculator = SpeedCalculator()
            val progressThrottler = ProgressThrottler(200) { progress ->
                onProgress(progress)
            }

            val result = if (supportsRange && config.threadCount > 1 && totalSize > 0) {
                downloadChunked(
                    taskId = taskId,
                    config = config,
                    totalSize = totalSize,
                    downloadedSize = downloadedSize,
                    speedCalculator = speedCalculator,
                    progressThrottler = progressThrottler
                )
            } else {
                downloadSingleThread(
                    taskId = taskId,
                    config = config,
                    totalSize = totalSize,
                    downloadedSize = downloadedSize,
                    speedCalculator = speedCalculator,
                    progressThrottler = progressThrottler
                )
            }

            when (result) {
                is DownloadResult.Success -> {
                    val saveFile = File(config.savePath)
                    saveFile.parentFile?.mkdirs()
                    if (tempFile.renameTo(saveFile)) {
                        DownloadResult.Success(config.savePath, totalSize)
                    } else {
                        DownloadResult.Error("Failed to rename temp file")
                    }
                }
                else -> result
            }
        } catch (e: Exception) {
            DownloadResult.Error(e.message ?: "Unknown error", e)
        }
    }

    private suspend fun downloadChunked(
        taskId: String,
        config: DownloadTaskConfig,
        totalSize: Long,
        downloadedSize: AtomicLong,
        speedCalculator: SpeedCalculator,
        progressThrottler: ProgressThrottler
    ): DownloadResult = coroutineScope {
        val chunkSize = ceil(totalSize.toDouble() / config.threadCount).toLong()
        val tempFile = File(config.tempPath)

        if (tempFile.length() != totalSize) {
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.setLength(totalSize)
            }
        }

        val chunkTokenBucket = if (config.speedLimit > 0) {
            TokenBucket(config.speedLimit)
        } else null

        val jobs = mutableListOf<Job>()
        val mutex = Mutex()
        var errorResult: DownloadResult.Error? = null

        for (i in 0 until config.threadCount) {
            val startByte = i * chunkSize
            val endByte = minOf((i + 1) * chunkSize - 1, totalSize - 1)

            if (startByte > endByte) continue

            val job = launch(Dispatchers.IO) {
                try {
                    downloadChunk(
                        taskId = taskId,
                        url = config.url,
                        tempFile = tempFile,
                        startByte = startByte,
                        endByte = endByte,
                        downloadedSize = downloadedSize,
                        totalSize = totalSize,
                        chunkTokenBucket = chunkTokenBucket,
                        speedCalculator = speedCalculator,
                        progressThrottler = progressThrottler,
                        mutex = mutex
                    )
                } catch (e: Exception) {
                    mutex.withLock {
                        if (errorResult == null) {
                            errorResult = DownloadResult.Error(e.message ?: "Chunk download failed", e)
                        }
                    }
                }
            }
            jobs.add(job)
        }

        jobs.joinAll()

        when {
            isCancelled.get() -> {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                DownloadResult.Cancelled(true)
            }
            isPaused.get() -> {
                DownloadResult.Paused(downloadedSize.get(), totalSize)
            }
            errorResult != null -> {
                errorResult!!
            }
            else -> {
                progressThrottler.emitFinal(
                    DownloadProgress(
                        taskId = taskId,
                        downloadedSize = totalSize,
                        totalSize = totalSize,
                        speed = 0L,
                        status = DownloadStatus.COMPLETED
                    )
                )
                DownloadResult.Success(config.tempPath, totalSize)
            }
        }
    }

    private suspend fun downloadChunk(
        taskId: String,
        url: String,
        tempFile: File,
        startByte: Long,
        endByte: Long,
        downloadedSize: AtomicLong,
        totalSize: Long,
        chunkTokenBucket: TokenBucket?,
        speedCalculator: SpeedCalculator,
        progressThrottler: ProgressThrottler,
        mutex: Mutex
    ) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$startByte-$endByte")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val inputStream = body.byteStream()

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(startByte)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var chunkOffset = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    while (isPaused.get() && !isCancelled.get()) {
                        delay(100)
                    }

                    if (isCancelled.get()) {
                        return@use
                    }

                    if (chunkTokenBucket != null && chunkTokenBucket.getRate() > 0) {
                        chunkTokenBucket.consumeBlocking(bytesRead.toLong())
                    }
                    if (globalTokenBucket.getRate() > 0) {
                        globalTokenBucket.consumeBlocking(bytesRead.toLong())
                    }

                    raf.write(buffer, 0, bytesRead)
                    chunkOffset += bytesRead
                    downloadedSize.addAndGet(bytesRead.toLong())
                    speedCalculator.addBytes(bytesRead.toLong())

                    val speed = speedCalculator.getSpeed()
                    progressThrottler.update(
                        DownloadProgress(
                            taskId = taskId,
                            downloadedSize = downloadedSize.get(),
                            totalSize = totalSize,
                            speed = speed,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )
                }
            }
        }
    }

    private suspend fun downloadSingleThread(
        taskId: String,
        config: DownloadTaskConfig,
        totalSize: Long,
        downloadedSize: AtomicLong,
        speedCalculator: SpeedCalculator,
        progressThrottler: ProgressThrottler
    ): DownloadResult = withContext(Dispatchers.IO) {
        val tempFile = File(config.tempPath)
        val existingSize = tempFile.length()

        val request = Request.Builder()
            .url(config.url)
            .apply {
                if (existingSize > 0 && totalSize > 0) {
                    header("Range", "bytes=$existingSize-")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext DownloadResult.Error("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: return@withContext DownloadResult.Error("Empty response body")
            val inputStream = body.byteStream()

            val mode = if (existingSize > 0 && response.code == 206) "rw" else "rw"
            RandomAccessFile(tempFile, mode).use { raf ->
                if (existingSize == 0 || response.code != 206) {
                    raf.setLength(0)
                } else {
                    raf.seek(existingSize)
                }

                val buffer = ByteArray(8192)
                var bytesRead: Int
                val chunkTokenBucket = if (config.speedLimit > 0) {
                    TokenBucket(config.speedLimit)
                } else null

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    while (isPaused.get() && !isCancelled.get()) {
                        delay(100)
                    }

                    if (isCancelled.get()) {
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }
                        return@withContext DownloadResult.Cancelled(true)
                    }

                    if (chunkTokenBucket != null && chunkTokenBucket.getRate() > 0) {
                        chunkTokenBucket.consumeBlocking(bytesRead.toLong())
                    }
                    if (globalTokenBucket.getRate() > 0) {
                        globalTokenBucket.consumeBlocking(bytesRead.toLong())
                    }

                    raf.write(buffer, 0, bytesRead)
                    downloadedSize.addAndGet(bytesRead.toLong())
                    speedCalculator.addBytes(bytesRead.toLong())

                    val speed = speedCalculator.getSpeed()
                    progressThrottler.update(
                        DownloadProgress(
                            taskId = taskId,
                            downloadedSize = downloadedSize.get(),
                            totalSize = totalSize,
                            speed = speed,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )
                }
            }

            when {
                isCancelled.get() -> DownloadResult.Cancelled(true)
                isPaused.get() -> DownloadResult.Paused(downloadedSize.get(), totalSize)
                else -> {
                    progressThrottler.emitFinal(
                        DownloadProgress(
                            taskId = taskId,
                            downloadedSize = downloadedSize.get(),
                            totalSize = totalSize,
                            speed = 0L,
                            status = DownloadStatus.COMPLETED
                        )
                    )
                    DownloadResult.Success(config.tempPath, totalSize)
                }
            }
        }
    }

    private suspend fun checkFileInfo(url: String): FileInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val acceptRanges = response.header("Accept-Ranges")
            val supportsRange = acceptRanges?.contains("bytes") == true

            FileInfo(contentLength, supportsRange)
        }
    }

    private fun getExistingDownloadedSize(tempFile: File): Long {
        return if (tempFile.exists()) tempFile.length() else 0L
    }

    private data class FileInfo(
        val totalSize: Long,
        val supportsRange: Boolean
    )

    private class SpeedCalculator {
        private val bytesWindow = ArrayDeque<Pair<Long, Long>>()
        private val windowDurationMs = 2000L

        @Synchronized
        fun addBytes(bytes: Long) {
            val now = System.currentTimeMillis()
            bytesWindow.addLast(now to bytes)
            while (bytesWindow.isNotEmpty() && now - bytesWindow.first().first > windowDurationMs) {
                bytesWindow.removeFirst()
            }
        }

        @Synchronized
        fun getSpeed(): Long {
            val now = System.currentTimeMillis()
            while (bytesWindow.isNotEmpty() && now - bytesWindow.first().first > windowDurationMs) {
                bytesWindow.removeFirst()
            }
            if (bytesWindow.isEmpty()) return 0L
            val totalBytes = bytesWindow.sumOf { it.second }
            val timeSpanMs = (now - bytesWindow.first().first).coerceAtLeast(100L)
            return (totalBytes * 1000L) / timeSpanMs
        }
    }

    private class ProgressThrottler(
        private val intervalMs: Long,
        private val onEmit: (DownloadProgress) -> Unit
    ) {
        private var lastEmitTime = 0L
        private var lastProgress: DownloadProgress? = null

        fun update(progress: DownloadProgress) {
            lastProgress = progress
            val now = System.currentTimeMillis()
            if (now - lastEmitTime >= intervalMs) {
                lastEmitTime = now
                onEmit(progress)
            }
        }

        fun emitFinal(progress: DownloadProgress) {
            lastProgress = progress
            onEmit(progress)
        }
    }
}
