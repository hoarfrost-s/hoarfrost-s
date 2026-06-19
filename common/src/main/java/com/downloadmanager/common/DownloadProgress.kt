package com.downloadmanager.common

data class DownloadProgress(
    val taskId: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speed: Long, // bytes per second
    val segments: List<SegmentProgress>
)

data class SegmentProgress(
    val segmentIndex: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
)