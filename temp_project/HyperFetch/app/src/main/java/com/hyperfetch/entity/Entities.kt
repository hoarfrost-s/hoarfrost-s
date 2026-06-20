package com.hyperfetch.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hyperfetch.model.Category
import com.hyperfetch.model.Priority
import com.hyperfetch.model.TaskStatus

/**
 * 下载任务数据库实体
 */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val totalSize: Long = 0L,
    val downloaded: Long = 0L,
    val category: String = Category.OTHER.name,
    val status: String = TaskStatus.QUEUED.name,
    val priority: String = Priority.NORMAL.name,
    val threadCount: Int = 4,
    val speedLimit: Long = Long.MAX_VALUE,
    val savePath: String = "",
    val createTime: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * 任务分块数据库实体
 */
@Entity(
    tableName = "task_chunks",
    primaryKeys = ["taskId", "index"]
)
data class TaskChunkEntity(
    val taskId: String,
    val index: Int,
    val start: Long,
    val end: Long,
    val downloaded: Long = 0L,
    val status: String = "PENDING"
)

/**
 * 配置实体
 */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
