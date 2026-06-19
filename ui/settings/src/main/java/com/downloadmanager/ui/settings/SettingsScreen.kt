package com.downloadmanager.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.downloadmanager.common.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val threadCount by viewModel.threadCount.collectAsState()
    val maxConcurrency by viewModel.maxConcurrency.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val cellularAutoLimit by viewModel.cellularAutoLimit.collectAsState()
    val maxRetries by viewModel.maxRetries.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Download section
            SectionTitle("下载")
            SliderSetting(
                title = "默认线程数",
                value = threadCount,
                onValueChange = { viewModel.setThreadCount(it.toInt()) },
                valueRange = 1f..9f,
                steps = 7,
                displayValue = "${threadCount}线程"
            )
            SliderSetting(
                title = "最大并行任务",
                value = maxConcurrency,
                onValueChange = { viewModel.setMaxConcurrency(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                displayValue = "${maxConcurrency}个"
            )
            SliderSetting(
                title = "最大重试次数",
                value = maxRetries,
                onValueChange = { viewModel.setMaxRetries(it.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
                displayValue = "${maxRetries}次"
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Speed section
            SectionTitle("速度限制")
            SliderSetting(
                title = "全局速度限制",
                value = if (speedLimit == 0L) 0f else (speedLimit / 1024).toFloat(),
                onValueChange = {
                    val kb = it.toLong() * 1024
                    viewModel.setSpeedLimit(if (kb < 16 * 1024) 0L else kb)
                },
                valueRange = 0f..10240f,
                displayValue = if (speedLimit == 0L) "不限速" else FormatUtils.formatSpeed(speedLimit)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Network section
            SectionTitle("网络")
            SwitchSetting(
                title = "蜂窝网络自动限速",
                subtitle = "使用移动数据时自动限制下载速度",
                checked = cellularAutoLimit,
                onCheckedChange = { viewModel.setCellularAutoLimit(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Theme section
            SectionTitle("界面")
            Text(
                text = "主题设置",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                listOf("跟随系统", "浅色", "深色").forEachIndexed { index, label ->
                    androidx.compose.material3.FilterChip(
                        selected = themeMode == index,
                        onClick = { viewModel.setThemeMode(index) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}