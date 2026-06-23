package com.downloadmanager.app.engine

import com.downloadmanager.app.data.entity.DownloadStatus

data class DownloadProgress(
    val taskId: String,
    val downloadedSize: Long,
    val totalSize: Long,
    val speed: Long,
    val status: DownloadStatus,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalSize <= 0) {
            0f
        } else {
            (downloadedSize.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
        }
}
