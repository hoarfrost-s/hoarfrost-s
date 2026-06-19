package com.downloadmanager.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FormatUtils
import com.downloadmanager.common.SegmentProgress
import com.downloadmanager.common.TaskStatus

@Composable
fun TaskCard(
    task: DownloadTask,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlayPause: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Top row: file name, status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryEmoji(task.category.displayName),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = task.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.category.displayName} · ${FormatUtils.formatFileSize(task.fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segmented progress bar
            val dummySegments = List(task.threadCount) { index ->
                val segSize = if (task.fileSize > 0) task.fileSize / task.threadCount else 0
                SegmentProgress(
                    segmentIndex = index,
                    downloadedBytes = if (task.fileSize > 0) task.downloadedBytes / task.threadCount else 0,
                    totalBytes = segSize
                )
            }
            SegmentedProgressBar(segments = dummySegments)

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: speed, ETA, actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildStatusText(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    if (task.status.isActive || task.status.isWaiting) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (task.status == TaskStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Stop,
                                contentDescription = if (task.status == TaskStatus.PAUSED) "恢复" else "暂停",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        TaskStatus.PAUSED -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
        TaskStatus.QUEUED, TaskStatus.WAITING -> MaterialTheme.colorScheme.secondary
        TaskStatus.MERGING -> MaterialTheme.colorScheme.tertiary
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.displayName,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun categoryEmoji(category: String): String = when (category) {
    "视频" -> "🎬"
    "音频" -> "🎵"
    "压缩包" -> "📦"
    "文档" -> "📄"
    "图片" -> "🖼"
    "可执行" -> "📱"
    else -> "📁"
}

private fun buildStatusText(task: DownloadTask): String {
    return when (task.status) {
        TaskStatus.DOWNLOADING -> {
            val progress = if (task.fileSize > 0) "${(task.downloadedBytes * 100 / task.fileSize)}%" else ""
            "$progress · ${FormatUtils.formatFileSize(task.downloadedBytes)}/${FormatUtils.formatFileSize(task.fileSize)}"
        }
        TaskStatus.COMPLETED -> FormatUtils.formatFileSize(task.fileSize)
        TaskStatus.QUEUED -> "排队 #${task.queuePosition}"
        TaskStatus.PAUSED -> "已暂停 · ${FormatUtils.formatFileSize(task.downloadedBytes)}/${FormatUtils.formatFileSize(task.fileSize)}"
        TaskStatus.FAILED -> task.errorMessage ?: "下载失败"
        TaskStatus.MERGING -> "合并中..."
        else -> task.status.displayName
    }
}