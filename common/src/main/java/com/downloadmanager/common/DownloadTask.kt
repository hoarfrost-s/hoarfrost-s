package com.downloadmanager.common

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long = -1,
    val downloadedBytes: Long = 0,
    val category: FileCategory = FileCategory.OTHER,
    val status: TaskStatus = TaskStatus.WAITING,
    val threadCount: Int = 4,
    val savePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val queuePosition: Int = -1,
    val errorMessage: String? = null,
    val linkInfo: LinkInfo? = null
)

data class LinkInfo(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String?,
    val category: FileCategory,
    val threadCount: Int,
    val savePath: String,
    val createdAt: Long
)