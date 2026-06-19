package com.hyperfetch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperfetch.model.Category
import com.hyperfetch.model.DownloadTask
import com.hyperfetch.ui.components.NewTaskBottomSheet
import com.hyperfetch.ui.components.TaskCard
import com.hyperfetch.ui.theme.*

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tasks: List<DownloadTask>,
    filteredTasks: List<DownloadTask>,
    categoryStats: Map<Category, Int>,
    selectedCategory: Category?,
    totalSpeed: Long,
    downloadingCount: Int,
    onSelectCategory: (Category?) -> Unit,
    onCreateTask: (String, com.hyperfetch.model.DownloadOptions) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HyperFetch",
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* 搜索 */ }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        bottomBar = {
            BottomStatsBar(
                concurrentCount = downloadingCount,
                totalSpeed = totalSpeed,
                taskCount = tasks.size
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskSheet = true },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建任务")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 分类 Tab
            CategoryTab(
                selectedCategory = selectedCategory,
                categoryStats = categoryStats,
                onSelectCategory = onSelectCategory
            )

            // 任务列表
            if (filteredTasks.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onPause = { onPauseTask(task.id) },
                            onResume = { onResumeTask(task.id) },
                            onDelete = { onDeleteTask(task.id) },
                            onRetry = { onRetryTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // 新建任务 BottomSheet
    if (showNewTaskSheet) {
        NewTaskBottomSheet(
            onDismiss = { showNewTaskSheet = false },
            onConfirm = { url, options ->
                onCreateTask(url, options)
                showNewTaskSheet = false
            }
        )
    }

    // 设置页面
    if (showSettings) {
        SettingsBottomSheet(
            onDismiss = { showSettings = false }
        )
    }
}

/**
 * 分类 Tab
 */
@Composable
private fun CategoryTab(
    selectedCategory: Category?,
    categoryStats: Map<Category, Int>,
    onSelectCategory: (Category?) -> Unit
) {
    val categories = listOf(
        null to "全部",
        Category.VIDEO to "视频",
        Category.AUDIO to "音频",
        Category.ARCHIVE to "压缩包",
        Category.OTHER to "其他"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { (category, label) ->
            val count = if (category == null) {
                categoryStats.values.sum()
            } else {
                categoryStats[category] ?: 0
            }

            CategoryTabItem(
                label = label,
                count = count,
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) }
            )
        }
    }
}

/**
 * 分类 Tab 项
 */
@Composable
private fun CategoryTabItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Primary else BackgroundSecondary,
        label = "tab_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) OnPrimary else TextSecondary,
        label = "tab_text"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 底部统计栏
 */
@Composable
private fun BottomStatsBar(
    concurrentCount: Int,
    totalSpeed: Long,
    taskCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BackgroundSecondary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = "$concurrentCount/5",
                label = "并发"
            )
            StatItem(
                value = formatSpeed(totalSpeed),
                label = "总速度"
            )
            StatItem(
                value = taskCount.toString(),
                label = "任务"
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextDisabled
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无下载任务",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮添加",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
        }
    }
}

/**
 * 设置 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    onDismiss: () -> Unit
) {
    var multiTaskEnabled by remember { mutableStateOf(true) }
    var maxConcurrent by remember { mutableIntStateOf(3) }
    var wifiOnly by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 多任务开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "开启多任务同时下载",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Switch(
                    checked = multiTaskEnabled,
                    onCheckedChange = { multiTaskEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 最大并发数
            if (multiTaskEnabled) {
                Text(
                    text = "最大同时下载任务数: $maxConcurrent",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Slider(
                    value = maxConcurrent.toFloat(),
                    onValueChange = { maxConcurrent = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = Divider
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 仅 Wi-Fi 下载
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "仅 Wi-Fi 下载",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 电池优化按钮
            OutlinedButton(
                onClick = { /* 打开电池优化设置 */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("电池优化白名单")
            }
        }
    }
}

/**
 * 格式化速度
 */
private fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var size = bytesPerSecond.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
