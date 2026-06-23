package com.downloadmanager.app.data.entity

enum class DownloadStatus(val value: String) {
    PENDING("pending"),
    DOWNLOADING("downloading"),
    PAUSED("paused"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    WAITING("waiting");

    companion object {
        fun fromValue(value: String): DownloadStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

data class DownloadTaskEntity(
    val id: String,
    val url: String,
    val fileName: String,
    val originalName: String,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val threadCount: Int = 3,
    val speedLimit: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val savePath: String,
    val tempPath: String,
    val mimeType: String? = null,
    val md5Hash: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)
