package com.downloadmanager.app.ui.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.R
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.ui.component.DownloadProgressBar
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun mapDownloadStatus(status: DownloadStatus): com.downloadmanager.app.ui.component.DownloadStatus {
    return when (status) {
        DownloadStatus.DOWNLOADING -> com.downloadmanager.app.ui.component.DownloadStatus.DOWNLOADING
        DownloadStatus.PAUSED -> com.downloadmanager.app.ui.component.DownloadStatus.PAUSED
        DownloadStatus.FAILED -> com.downloadmanager.app.ui.component.DownloadStatus.FAILED
        DownloadStatus.COMPLETED -> com.downloadmanager.app.ui.component.DownloadStatus.COMPLETED
        else -> com.downloadmanager.app.ui.component.DownloadStatus.DOWNLOADING
    }
}

private fun getCategoryIcon(categoryId: String?): ImageVector {
    return when (categoryId) {
        "video" -> Icons.Default.Videocam
        "audio" -> Icons.Default.MusicNote
        "image" -> Icons.Default.Photo
        "document" -> Icons.Default.Description
        "archive" -> Icons.Default.Archive
        "apk" -> Icons.Default.Smartphone
        else -> Icons.Default.Description
    }
}

private fun getCategoryName(categoryId: String?): Int {
    return when (categoryId) {
        "video" -> R.string.category_video
        "audio" -> R.string.category_audio
        "image" -> R.string.category_image
        "document" -> R.string.category_document
        "archive" -> R.string.category_archive
        "apk" -> R.string.category_apk
        else -> R.string.category_other
    }
}

private fun getStatusText(status: DownloadStatus): Int {
    return when (status) {
        DownloadStatus.PENDING -> R.string.status_pending
        DownloadStatus.DOWNLOADING -> R.string.status_downloading
        DownloadStatus.PAUSED -> R.string.status_paused
        DownloadStatus.COMPLETED -> R.string.status_completed
        DownloadStatus.FAILED -> R.string.status_failed
        DownloadStatus.CANCELLED -> R.string.status_cancelled
        DownloadStatus.WAITING -> R.string.status_waiting
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadTaskCard(
    task: DownloadTaskEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onTaskClick: () -> Unit,
    onTaskLongClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (task.totalSize > 0) {
        task.downloadedSize.toFloat() / task.totalSize.toFloat()
    } else {
        0f
    }

    val statusColor = when (task.status) {
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTaskClick,
                onLongClick = onTaskLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(task.categoryId),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(
                                    id = getCategoryName(task.categoryId)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = getStatusText(task.status)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DownloadProgressBar(
                progress = progress,
                status = mapDownloadStatus(task.status)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${FileUtils.formatFileSize(task.downloadedSize)} / ${FileUtils.formatFileSize(task.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.status == DownloadStatus.DOWNLOADING && task.totalSize > 0) {
                        val remainingBytes = task.totalSize - task.downloadedSize
                        val speed = 1024L * 1024
                        val remainingSeconds = if (speed > 0) remainingBytes / speed else 0
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = R.string.time_remaining,
                                FileUtils.formatDuration(remainingSeconds)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (task.status == DownloadStatus.COMPLETED && task.completedAt != null) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = R.string.completed_at,
                                formatDateTime(task.completedAt)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.status == DownloadStatus.FAILED && !task.errorMessage.isNullOrEmpty()) {
                        Text(
                            text = task.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.WAITING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.action_pause),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.PENDING -> {
                            IconButton(onClick = onResumeClick) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.action_start),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetryClick) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.action_retry),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onShareClick) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.action_share),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {}
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = androidx.compose.ui.res.stringResource(id = R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DownloadTaskCardDownloadingPreview() {
    DownloadManagerTheme {
        DownloadTaskCard(
            task = DownloadTaskEntity(
                id = "1",
                url = "https://example.com/video.mp4",
                fileName = "示例视频文件.mp4",
                originalName = "示例视频文件.mp4",
                totalSize = 1024L * 1024 * 100,
                downloadedSize = 1024L * 1024 * 65,
                status = DownloadStatus.DOWNLOADING,
                categoryId = "video",
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            isSelected = false,
            isMultiSelectMode = false,
            onTaskClick = {},
            onTaskLongClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onDeleteClick = {},
            onShareClick = {},
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun DownloadTaskCardCompletedPreview() {
    DownloadManagerTheme {
        DownloadTaskCard(
            task = DownloadTaskEntity(
                id = "2",
                url = "https://example.com/document.pdf",
                fileName = "项目文档.pdf",
                originalName = "项目文档.pdf",
                totalSize = 1024L * 1024 * 5,
                downloadedSize = 1024L * 1024 * 5,
                status = DownloadStatus.COMPLETED,
                categoryId = "document",
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis() - 3600000,
                completedAt = System.currentTimeMillis() - 3600000
            ),
            isSelected = false,
            isMultiSelectMode = false,
            onTaskClick = {},
            onTaskLongClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onDeleteClick = {},
            onShareClick = {},
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun DownloadTaskCardPausedPreview() {
    DownloadManagerTheme {
        DownloadTaskCard(
            task = DownloadTaskEntity(
                id = "3",
                url = "https://example.com/music.mp3",
                fileName = "音乐文件.mp3",
                originalName = "音乐文件.mp3",
                totalSize = 1024L * 1024 * 10,
                downloadedSize = 1024L * 1024 * 3,
                status = DownloadStatus.PAUSED,
                categoryId = "audio",
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            isSelected = false,
            isMultiSelectMode = false,
            onTaskClick = {},
            onTaskLongClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onDeleteClick = {},
            onShareClick = {},
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun DownloadTaskCardFailedPreview() {
    DownloadManagerTheme {
        DownloadTaskCard(
            task = DownloadTaskEntity(
                id = "4",
                url = "https://example.com/image.jpg",
                fileName = "图片文件.jpg",
                originalName = "图片文件.jpg",
                totalSize = 1024L * 1024 * 2,
                downloadedSize = 1024L * 1024 * 1,
                status = DownloadStatus.FAILED,
                categoryId = "image",
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                errorMessage = "网络连接失败",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            isSelected = false,
            isMultiSelectMode = false,
            onTaskClick = {},
            onTaskLongClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onDeleteClick = {},
            onShareClick = {},
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun DownloadTaskCardSelectedPreview() {
    DownloadManagerTheme {
        DownloadTaskCard(
            task = DownloadTaskEntity(
                id = "5",
                url = "https://example.com/file.zip",
                fileName = "压缩文件.zip",
                originalName = "压缩文件.zip",
                totalSize = 1024L * 1024 * 50,
                downloadedSize = 1024L * 1024 * 25,
                status = DownloadStatus.DOWNLOADING,
                categoryId = "archive",
                savePath = "/downloads",
                tempPath = "/downloads/temp",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            isSelected = true,
            isMultiSelectMode = true,
            onTaskClick = {},
            onTaskLongClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onDeleteClick = {},
            onShareClick = {},
            onRetryClick = {}
        )
    }
}
