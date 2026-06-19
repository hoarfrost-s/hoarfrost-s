package com.downloadmanager.engine

import com.downloadmanager.common.DownloadProgress
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FileUtils
import com.downloadmanager.common.Segment
import com.downloadmanager.common.TaskStatus
import com.downloadmanager.common.UrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class DefaultDownloadEngine(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RateLimiter
) : DownloadEngine {

    private val segmentDispatcher = SegmentDispatcher(okHttpClient, rateLimiter)

    override suspend fun execute(task: DownloadTask): Flow<DownloadProgress> = flow {
        val tempDir = File(task.savePath).parentFile?.let {
            FileUtils.getTempDir(it, task.id)
        } ?: throw IllegalStateException("Invalid save path")

        // Get file size if unknown
        var fileSize = task.fileSize
        if (fileSize <= 0) {
            fileSize = fetchFileSize(task.url)
        }

        val segments = if (task.threadCount > 1 && fileSize > 0) {
            calculateSegments(fileSize, task.threadCount, task.downloadedBytes)
        } else {
            listOf(Segment(0, 0, fileSize - 1, task.downloadedBytes))
        }

        segmentDispatcher.download(task, segments, tempDir) { progress ->
            emit(progress)
        }

        // Merge segments
        val segmentFiles = segments.map { File(tempDir, "seg_${it.index}.tmp") }
        val outputFile = File(task.savePath)
        FileUtils.mergeFiles(segmentFiles, outputFile)

        emit(
            DownloadProgress(
                taskId = task.id,
                totalBytes = fileSize,
                downloadedBytes = fileSize,
                speed = 0,
                segments = segments.map { SegmentProgress(it.index, it.totalBytes, it.totalBytes) }
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun pause(taskId: String) {
        segmentDispatcher.pauseTask(taskId)
    }

    override suspend fun resume(taskId: String) {
        segmentDispatcher.resumeTask(taskId)
    }

    override suspend fun cancel(taskId: String) {
        segmentDispatcher.cancelTask(taskId)
    }

    private suspend fun fetchFileSize(url: String): Long {
        return try {
            val request = Request.Builder().url(url).head().build()
            val response = okHttpClient.newCall(request).execute()
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
            response.close()
            contentLength
        } catch (e: Exception) {
            -1L
        }
    }

    private fun calculateSegments(fileSize: Long, threadCount: Int, downloadedBytes: Long): List<Segment> {
        val segmentSize = fileSize / threadCount
        return (0 until threadCount).map { index ->
            val start = index * segmentSize
            val end = if (index == threadCount - 1) fileSize - 1 else start + segmentSize - 1
            val segDownloaded = if (downloadedBytes > 0) {
                val segEnd = end + 1
                when {
                    downloadedBytes <= start -> 0L
                    downloadedBytes >= segEnd -> segEnd - start
                    else -> downloadedBytes - start
                }
            } else 0L
            Segment(index, start, end, segDownloaded)
        }
    }
}