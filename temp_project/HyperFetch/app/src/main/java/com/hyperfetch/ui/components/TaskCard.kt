package com.hyperfetch.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperfetch.model.Category
import com.hyperfetch.model.DownloadTask
import com.hyperfetch.model.TaskStatus
import com.hyperfetch.ui.theme.*

/**
 * 任务卡片组件
 */
@Composable
fun TaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(task.category)
    val statusColor = getStatusColor(task.status)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 第一行：类型图标、文件名、状态徽章
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型徽章
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryBadge(task.category),
                        color = categoryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 文件名
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 状态徽章
                StatusBadge(status = task.status, color = statusColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：大小、线程数、速度、剩余时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatFileSize(task.totalSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Text(
                    text = "${task.threadCount}线程",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (task.isDownloading) {
                    Text(
                        text = formatSpeed(task.speed),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (task.eta > 0 && task.isDownloading) {
                    Text(
                        text = formatEta(task.eta),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 进度条
            AnimatedProgressBar(
                progress = task.progress,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 第三行：已下载、操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatFileSize(task.downloaded)} / ${formatFileSize(task.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (task.status) {
                        TaskStatus.DOWNLOADING -> {
                            IconButton(
                                onClick = onPause,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "暂停",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        TaskStatus.PAUSED, TaskStatus.QUEUED -> {
                            IconButton(
                                onClick = onResume,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "继续",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        TaskStatus.FAILED -> {
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "重试",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 带动画的进度条
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = modifier
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ProgressTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, Color(0xFFFFD089))
                    )
                )
        )
    }
}

/**
 * 状态徽章
 */
@Composable
fun StatusBadge(status: TaskStatus, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = getStatusText(status),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 获取分类徽章文字
 */
private fun getCategoryBadge(category: Category): String {
    return when (category) {
        Category.VIDEO -> "MP4"
        Category.AUDIO -> "MP3"
        Category.ARCHIVE -> "ZIP"
        Category.DOCUMENT -> "DOC"
        Category.PROGRAM -> "APK"
        Category.OTHER -> "FILE"
    }
}

/**
 * 获取分类颜色
 */
private fun getCategoryColor(category: Category): Color {
    return when (category) {
        Category.VIDEO -> CategoryVideo
        Category.AUDIO -> CategoryAudio
        Category.ARCHIVE -> CategoryArchive
        Category.DOCUMENT -> CategoryDocument
        Category.PROGRAM -> CategoryProgram
        Category.OTHER -> CategoryOther
    }
}

/**
 * 获取状态颜色
 */
private fun getStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.QUEUED -> TextSecondary
        TaskStatus.DOWNLOADING -> Primary
        TaskStatus.PAUSED -> Secondary
        TaskStatus.COMPLETED -> Success
        TaskStatus.FAILED -> Error
    }
}

/**
 * 获取状态文本
 */
private fun getStatusText(status: TaskStatus): String {
    return when (status) {
        TaskStatus.QUEUED -> "排队中"
        TaskStatus.DOWNLOADING -> "下载中"
        TaskStatus.PAUSED -> "已暂停"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.FAILED -> "失败"
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${size.toInt()} ${units[unitIndex]}"
    } else {
        "%.1f %s".format(size, units[unitIndex])
    }
}

/**
 * 格式化速度
 */
private fun formatSpeed(bytesPerSecond: Long): String {
    return "${formatFileSize(bytesPerSecond)}/s"
}

/**
 * 格式化剩余时间
 */
private fun formatEta(seconds: Long): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
