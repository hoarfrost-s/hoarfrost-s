package com.downloadmanager.app.engine

import com.downloadmanager.app.data.entity.DownloadStatus

sealed class DownloadResult {
    data class Success(
        val filePath: String,
        val totalSize: Long
    ) : DownloadResult()

    data class Paused(
        val downloadedSize: Long,
        val totalSize: Long
    ) : DownloadResult()

    data class Cancelled(
        val deleteFile: Boolean
    ) : DownloadResult()

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : DownloadResult()
}
