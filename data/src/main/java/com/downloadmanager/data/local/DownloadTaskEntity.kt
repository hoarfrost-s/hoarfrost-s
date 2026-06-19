package com.downloadmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val category: String,
    val status: String,
    val threadCount: Int,
    val savePath: String,
    val createdAt: Long,
    val queuePosition: Int,
    val errorMessage: String? = null,
    val contentType: String? = null
)