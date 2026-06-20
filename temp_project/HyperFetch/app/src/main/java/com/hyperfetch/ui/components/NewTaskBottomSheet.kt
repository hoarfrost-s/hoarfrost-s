package com.hyperfetch.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperfetch.model.Category
import com.hyperfetch.model.DownloadOptions
import com.hyperfetch.model.Priority
import com.hyperfetch.ui.theme.*

/**
 * 新建任务 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, DownloadOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var threadCount by remember { mutableIntStateOf(4) }
    var speedLimitEnabled by remember { mutableStateOf(false) }
    var speedLimit by remember { mutableLongStateOf(1024 * 1024L) } // 默认 1 MB/s
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var startNow by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val isUrlValid = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

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
                .verticalScroll(rememberScrollState())
        ) {
            // 标题
            Text(
                text = "新建下载任务",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // URL 输入框
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("下载链接") },
                placeholder = { Text("输入或粘贴链接") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { /* 粘贴 */ }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "粘贴")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Divider,
                    focusedLabelColor = Primary,
                    cursorColor = Primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 分类选择
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategorySelector(
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 线程数滑块
            Text(
                text = "线程数: $threadCount",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Slider(
                value = threadCount.toFloat(),
                onValueChange = { threadCount = it.toInt() },
                valueRange = 1f..9f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = Divider
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 速度限制开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "速度限制",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Switch(
                    checked = speedLimitEnabled,
                    onCheckedChange = { speedLimitEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f)
                    )
                )
            }

            if (speedLimitEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SpeedLimitSelector(
                    value = speedLimit,
                    onValueChange = { speedLimit = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 优先级选择
            Text(
                text = "优先级",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityChip(
                    text = "普通",
                    selected = priority == Priority.NORMAL,
                    onClick = { priority = Priority.NORMAL },
                    modifier = Modifier.weight(1f)
                )
                PriorityChip(
                    text = "高",
                    selected = priority == Priority.HIGH,
                    onClick = { priority = Priority.HIGH },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 立即开始开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "立即开始",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Switch(
                    checked = startNow,
                    onCheckedChange = { startNow = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 确认按钮
            Button(
                onClick = {
                    if (isUrlValid) {
                        onConfirm(
                            url,
                            DownloadOptions(
                                threadCount = threadCount,
                                speedLimit = if (speedLimitEnabled) speedLimit else Long.MAX_VALUE,
                                priority = priority,
                                startNow = startNow,
                                category = selectedCategory
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isUrlValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = Divider,
                    disabledContentColor = TextDisabled
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始下载", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 分类选择器
 */
@Composable
private fun CategorySelector(
    selected: Category?,
    onSelect: (Category?) -> Unit
) {
    val categories = listOf(
        null to "自动",
        Category.VIDEO to "视频",
        Category.AUDIO to "音频",
        Category.ARCHIVE to "压缩包",
        Category.DOCUMENT to "文档",
        Category.OTHER to "其他"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (category, label) ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = OnPrimary,
                    containerColor = Background,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

/**
 * 速度限制选择器
 */
@Composable
private fun SpeedLimitSelector(
    value: Long,
    onValueChange: (Long) -> Unit
) {
    val options = listOf(
        64 * 1024L to "64 KB/s",
        128 * 1024L to "128 KB/s",
        256 * 1024L to "256 KB/s",
        512 * 1024L to "512 KB/s",
        1024 * 1024L to "1 MB/s",
        2 * 1024 * 1024L to "2 MB/s",
        5 * 1024 * 1024L to "5 MB/s"
    )

    Column {
        options.forEach { (speed, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = value == speed,
                    onClick = { onValueChange(speed) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Primary
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * 优先级标签
 */
@Composable
private fun PriorityChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = OnPrimary,
            containerColor = Background,
            labelColor = TextSecondary
        )
    )
}
