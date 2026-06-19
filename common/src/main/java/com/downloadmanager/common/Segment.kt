package com.downloadmanager.common

data class Segment(
    val index: Int,
    val start: Long,
    val end: Long,
    val downloadedBytes: Long = 0
) {
    val totalBytes: Long get() = end - start + 1
    val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    val isComplete: Boolean get() = downloadedBytes >= totalBytes
}