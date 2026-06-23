package com.downloadmanager.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.ui.component.DownloadProgressBar
import com.downloadmanager.app.ui.component.DownloadStatus as UiDownloadStatus
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.util.FileUtils

@Composable
fun RecentTaskItem(
    task: DownloadTaskEntity,
    categoryName: String? = null,
    speed: Long = 0L,
    onPauseResumeClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onItemLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDownloading = task.status == DownloadStatus.DOWNLOADING
    val progress = if (task.totalSize > 0) {
        task.downloadedSize.toFloat() / task.totalSize.toFloat()
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (categoryName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            DownloadProgressBar(
                progress = progress,
                status = mapDownloadStatus(task.status)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = getStatusText(task.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = getStatusColor(task.status)
                    )
                    if (isDownloading && speed > 0) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FileUtils.formatSpeed(speed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        IconButton(
                            onClick = onPauseResumeClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "暂停",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
                        IconButton(
                            onClick = onPauseResumeClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "继续",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun mapDownloadStatus(status: DownloadStatus): UiDownloadStatus {
    return when (status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.PENDING, DownloadStatus.WAITING -> UiDownloadStatus.DOWNLOADING
        DownloadStatus.PAUSED -> UiDownloadStatus.PAUSED
        DownloadStatus.FAILED -> UiDownloadStatus.FAILED
        DownloadStatus.COMPLETED -> UiDownloadStatus.COMPLETED
        DownloadStatus.CANCELLED -> UiDownloadStatus.FAILED
    }
}

private fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.DOWNLOADING -> "下载中"
        DownloadStatus.PAUSED -> "已暂停"
        DownloadStatus.FAILED -> "下载失败"
        DownloadStatus.COMPLETED -> "已完成"
        DownloadStatus.PENDING -> "等待中"
        DownloadStatus.WAITING -> "排队中"
        DownloadStatus.CANCELLED -> "已取消"
    }
}

@Composable
private fun getStatusColor(status: DownloadStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.PENDING, DownloadStatus.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Preview
@Composable
private fun RecentTaskItemDownloadingPreview() {
    DownloadManagerTheme {
        RecentTaskItem(
            task = DownloadTaskEntity(
                id = "1",
                url = "https://example.com/file.zip",
                fileName = "example_file_name_very_long_name.zip",
                originalName = "example.zip",
                totalSize = 1024 * 1024 * 100,
                downloadedSize = 1024 * 1024 * 65,
                status = DownloadStatus.DOWNLOADING,
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            categoryName = "压缩包",
            speed = 1024 * 1024
        )
    }
}

@Preview
@Composable
private fun RecentTaskItemPausedPreview() {
    DownloadManagerTheme {
        RecentTaskItem(
            task = DownloadTaskEntity(
                id = "2",
                url = "https://example.com/video.mp4",
                fileName = "video.mp4",
                originalName = "video.mp4",
                totalSize = 1024 * 1024 * 500,
                downloadedSize = 1024 * 1024 * 200,
                status = DownloadStatus.PAUSED,
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            categoryName = "视频"
        )
    }
}

@Preview
@Composable
private fun RecentTaskItemCompletedPreview() {
    DownloadManagerTheme {
        RecentTaskItem(
            task = DownloadTaskEntity(
                id = "3",
                url = "https://example.com/music.mp3",
                fileName = "music.mp3",
                originalName = "music.mp3",
                totalSize = 1024 * 1024 * 5,
                downloadedSize = 1024 * 1024 * 5,
                status = DownloadStatus.COMPLETED,
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            categoryName = "音乐"
        )
    }
}
