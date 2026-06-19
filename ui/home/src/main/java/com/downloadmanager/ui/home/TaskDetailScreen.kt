package com.downloadmanager.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.downloadmanager.common.FormatUtils
import com.downloadmanager.common.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val task = allTasks.find { it.id == taskId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (task == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("任务不存在", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Link info section
            Text(
                text = "链接信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("文件名", task.fileName)
            InfoRow("URL", task.url)
            InfoRow("文件大小", FormatUtils.formatFileSize(task.fileSize))
            InfoRow("分类", task.category.displayName)
            InfoRow("Content-Type", task.linkInfo?.contentType ?: "未知")
            InfoRow("线程数", "${task.threadCount}线程")
            InfoRow("保存路径", task.savePath.ifBlank { "未设置" })
            InfoRow("创建时间", formatTime(task.createdAt))
            InfoRow("状态", task.status.displayName)

            if (task.errorMessage != null) {
                InfoRow("错误信息", task.errorMessage, isError = true)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Download progress section
            Text(
                text = "下载进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Overall progress
            val overallProgress = if (task.fileSize > 0) {
                (task.downloadedBytes * 100 / task.fileSize).toInt()
            } else 0

            InfoRow("总体进度", "$overallProgress%")
            InfoRow("已下载", FormatUtils.formatFileSize(task.downloadedBytes))
            InfoRow("总大小", FormatUtils.formatFileSize(task.fileSize))

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented progress
            if (task.status.isActive || task.status == TaskStatus.PAUSED) {
                Text(
                    text = "分段进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dummySegments = List(task.threadCount) { index ->
                    com.downloadmanager.common.SegmentProgress(
                        segmentIndex = index,
                        downloadedBytes = if (task.fileSize > 0) task.downloadedBytes / task.threadCount else 0,
                        totalBytes = if (task.fileSize > 0) task.fileSize / task.threadCount else 0
                    )
                }
                SegmentedProgressBar(segments = dummySegments)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    task.status == TaskStatus.PAUSED -> {
                        Button(
                            onClick = { viewModel.resumeTask(task.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("恢复下载")
                        }
                    }
                    task.status.isActive -> {
                        OutlinedButton(
                            onClick = { viewModel.pauseTask(task.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("暂停")
                        }
                    }
                    task.status == TaskStatus.FAILED -> {
                        Button(
                            onClick = { viewModel.resumeTask(task.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重试")
                        }
                    }
                }
                if (!task.status.isTerminal) {
                    OutlinedButton(
                        onClick = { viewModel.cancelTask(task.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消任务")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun formatTime(epochMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(epochMs))
    } catch (e: Exception) {
        "未知"
    }
}