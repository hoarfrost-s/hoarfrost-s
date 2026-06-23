package com.downloadmanager.app.engine

data class DownloadTaskConfig(
    val url: String,
    val fileName: String,
    val savePath: String,
    val tempPath: String,
    val threadCount: Int = 3,
    val speedLimit: Long = 0
)
