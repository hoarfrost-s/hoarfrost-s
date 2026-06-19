package com.downloadmanager.engine

import com.downloadmanager.common.DownloadProgress
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.Segment
import com.downloadmanager.common.SegmentProgress
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class SegmentDispatcher(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RateLimiter
) {
    private val pauseFlags = mutableMapOf<String, Boolean>()
    private val cancelFlags = mutableMapOf<String, Boolean>()

    suspend fun download(
        task: DownloadTask,
        segments: List<Segment>,
        tempDir: File,
        onProgress: suspend (DownloadProgress) -> Unit
    ) {
        val totalBytes = task.fileSize
        val downloadedBytes = AtomicLong(task.downloadedBytes)
        val segmentProgressMap = mutableMapOf<Int, SegmentProgress>()

        kotlinx.coroutines.coroutineScope {
            segments.map { segment ->
                kotlinx.coroutines.async {
                    downloadSegment(task, segment, tempDir, downloadedBytes, totalBytes) { progress ->
                        segmentProgressMap[segment.index] = progress
                        val allSegments = segments.indices.map { idx ->
                            segmentProgressMap[idx] ?: SegmentProgress(idx, 0, segments[idx].totalBytes)
                        }
                        onProgress(
                            DownloadProgress(
                                taskId = task.id,
                                totalBytes = totalBytes,
                                downloadedBytes = downloadedBytes.get(),
                                speed = 0,
                                segments = allSegments
                            )
                        )
                    }
                }
            }.forEach { it.await() }
        }
    }

    private suspend fun downloadSegment(
        task: DownloadTask,
        segment: Segment,
        tempDir: File,
        totalDownloaded: AtomicLong,
        totalBytes: Long,
        onProgress: suspend (SegmentProgress) -> Unit
    ) {
        val start = segment.start + segment.downloadedBytes
        if (start > segment.end) {
            onProgress(SegmentProgress(segment.index, segment.totalBytes, segment.totalBytes))
            return
        }

        val request = Request.Builder()
            .url(task.url)
            .header("Range", "bytes=$start-${segment.end}")
            .build()

        val call = okHttpClient.newCall(request)
        call.execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }

            response.body?.source()?.let { source ->
                val tempFile = File(tempDir, "seg_${segment.index}.tmp")
                if (start == segment.start) tempFile.delete()

                tempFile.outputStream().buffered().use { out ->
                    val buffer = ByteArray(8192)
                    var segmentDownloaded = segment.downloadedBytes
                    var bytesRead: Int

                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        if (isPaused(task.id)) {
                            call.cancel()
                            return
                        }
                        if (isCancelled(task.id)) {
                            call.cancel()
                            tempFile.delete()
                            return
                        }

                        rateLimiter.acquire(bytesRead.toLong())
                        out.write(buffer, 0, bytesRead)
                        segmentDownloaded += bytesRead
                        totalDownloaded.addAndGet(bytesRead.toLong())

                        onProgress(
                            SegmentProgress(
                                segmentIndex = segment.index,
                                downloadedBytes = segmentDownloaded,
                                totalBytes = segment.totalBytes - segment.downloadedBytes + segmentDownloaded
                            )
                        )
                    }
                }
            }
        }
    }

    fun pauseTask(taskId: String) {
        pauseFlags[taskId] = true
    }

    fun resumeTask(taskId: String) {
        pauseFlags[taskId] = false
    }

    fun cancelTask(taskId: String) {
        cancelFlags[taskId] = true
    }

    private fun isPaused(taskId: String): Boolean = pauseFlags[taskId] == true
    private fun isCancelled(taskId: String): Boolean = cancelFlags[taskId] == true

    fun cleanup(taskId: String) {
        pauseFlags.remove(taskId)
        cancelFlags.remove(taskId)
    }
}