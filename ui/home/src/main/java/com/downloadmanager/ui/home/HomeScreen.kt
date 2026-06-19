package com.downloadmanager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.downloadmanager.common.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCategory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("已选择 ${selectedTaskIds.size} 项")
                    } else {
                        Text("下载管理器", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        TextButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Text("取消")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加任务")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (!isMultiSelectMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索文件名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { viewModel.onStatusFilterChanged(null) },
                        label = { Text("全部") }
                    )
                    listOf(TaskStatus.DOWNLOADING, TaskStatus.QUEUED, TaskStatus.COMPLETED, TaskStatus.FAILED).forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { viewModel.onStatusFilterChanged(if (selectedStatus == status) null else status) },
                            label = { Text(status.displayName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task list
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无下载任务",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        viewModel.cancelTask(task.id)
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        if (task.status == TaskStatus.PAUSED) {
                                            viewModel.resumeTask(task.id)
                                        } else if (task.status.isActive) {
                                            viewModel.pauseTask(task.id)
                                        }
                                    }
                                    SwipeToDismissBoxValue.Settled -> {}
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            gestureEnabled = !isMultiSelectMode,
                            backgroundContent = {
                                // 左侧：红色背景，取消
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            Color(0xFFE53935),
                                            RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                        ),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "取消",
                                        tint = Color.White,
                                        modifier = Modifier.padding(start = 20.dp)
                                    )
                                }
                                // 右侧：橙色背景，暂停/恢复
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            Color(0xFFFFA726),
                                            RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                        ),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val isPaused = task.status == TaskStatus.PAUSED
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Stop,
                                        contentDescription = if (isPaused) "恢复" else "暂停",
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        ) {
                            TaskCard(
                                task = task,
                                isSelected = task.id in selectedTaskIds,
                                onCardClick = {
                                    if (isMultiSelectMode) {
                                        viewModel.toggleTaskSelection(task.id)
                                    } else {
                                        onNavigateToDetail(task.id)
                                    }
                                },
                                onLongClick = { viewModel.enterMultiSelectMode(task.id) },
                                onPlayPause = {
                                    if (task.status == TaskStatus.PAUSED) {
                                        viewModel.resumeTask(task.id)
                                    } else if (task.status.isActive) {
                                        viewModel.pauseTask(task.id)
                                    }
                                },
                                onCancel = { viewModel.cancelTask(task.id) }
                            )
                        }
                    }
                }
            }
        }

        // Multi-select bottom bar
        AnimatedVisibility(visible = isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                com.downloadmanager.ui.home.OutlinedButton(onClick = { viewModel.batchPause() }) {
                    Text("暂停选中")
                }
                com.downloadmanager.ui.home.OutlinedButton(onClick = { viewModel.batchCancel() }) {
                    Text("取消选中")
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskBottomSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { url ->
                viewModel.addTask(url)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun OutlinedButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        content()
    }
}