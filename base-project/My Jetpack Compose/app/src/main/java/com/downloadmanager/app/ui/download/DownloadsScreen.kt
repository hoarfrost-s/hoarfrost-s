package com.downloadmanager.app.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadmanager.app.R
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.ui.component.EmptyState
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
    categoryId: String? = null,
    initialFilterStatus: DownloadStatus? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember {
        val initialTab = DownloadTab.entries.firstOrNull { it.status == initialFilterStatus }
            ?: DownloadTab.ALL
        mutableIntStateOf(DownloadTab.entries.indexOf(initialTab))
    }

    val currentTab = DownloadTab.entries[selectedTabIndex]

    val filteredTasks = remember(uiState.tasks, currentTab, categoryId) {
        var tasks = uiState.tasks
        if (currentTab.status != null) {
            tasks = tasks.filter { it.status == currentTab.status }
        }
        if (categoryId != null) {
            tasks = tasks.filter { it.categoryId == categoryId }
        }
        tasks
    }

    val selectedTasks = remember(filteredTasks, uiState.selectedTaskIds) {
        filteredTasks.filter { uiState.selectedTaskIds.contains(it.id) }
    }

    val canStart = selectedTasks.any {
        it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.PENDING || it.status == DownloadStatus.FAILED
    }
    val canPause = selectedTasks.any {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.WAITING
    }
    val canShare = selectedTasks.all { it.status == DownloadStatus.COMPLETED } && selectedTasks.isNotEmpty()

    val allVisibleSelected = filteredTasks.isNotEmpty() &&
            filteredTasks.all { uiState.selectedTaskIds.contains(it.id) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                id = R.string.selected_count,
                                uiState.selectedTaskIds.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (allVisibleSelected) {
                                    filteredTasks.forEach { viewModel.toggleTaskSelection(it.id) }
                                } else {
                                    filteredTasks.forEach { task ->
                                        if (!uiState.selectedTaskIds.contains(task.id)) {
                                            viewModel.toggleTaskSelection(task.id)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = if (allVisibleSelected) {
                                    stringResource(id = R.string.content_desc_unselect_all)
                                } else {
                                    stringResource(id = R.string.content_desc_select_all)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.nav_downloads)) },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(id = R.string.content_desc_search)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (uiState.isMultiSelectMode) {
                MultiSelectBar(
                    selectedCount = uiState.selectedTaskIds.size,
                    totalCount = filteredTasks.size,
                    canStart = canStart,
                    canPause = canPause,
                    canShare = canShare,
                    onSelectAll = {
                        if (allVisibleSelected) {
                            filteredTasks.forEach { viewModel.toggleTaskSelection(it.id) }
                        } else {
                            filteredTasks.forEach { task ->
                                if (!uiState.selectedTaskIds.contains(task.id)) {
                                    viewModel.toggleTaskSelection(task.id)
                                }
                            }
                        }
                    },
                    onStart = {
                        selectedTasks.forEach { task ->
                            when (task.status) {
                                DownloadStatus.PAUSED, DownloadStatus.PENDING -> viewModel.resumeDownload(task.id)
                                DownloadStatus.FAILED -> viewModel.retryDownload(task.id)
                                else -> {}
                            }
                        }
                        viewModel.clearSelection()
                    },
                    onPause = {
                        selectedTasks.forEach { task ->
                            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.WAITING) {
                                viewModel.pauseDownload(task.id)
                            }
                        }
                        viewModel.clearSelection()
                    },
                    onDelete = {
                        viewModel.deleteSelectedTasks()
                    },
                    onShare = {}
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.isMultiSelectMode) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    DownloadTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                viewModel.setFilterStatus(tab.status)
                            },
                            text = {
                                Text(
                                    text = stringResource(id = tab.titleRes),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredTasks.isEmpty()) {
                    EmptyStateContent(tab = currentTab)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredTasks,
                            key = { it.id }
                        ) { task ->
                            DownloadTaskCard(
                                task = task,
                                isSelected = uiState.selectedTaskIds.contains(task.id),
                                isMultiSelectMode = uiState.isMultiSelectMode,
                                onTaskClick = {
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleTaskSelection(task.id)
                                    }
                                },
                                onTaskLongClick = {
                                    if (!uiState.isMultiSelectMode) {
                                        viewModel.enterMultiSelectMode(task.id)
                                    }
                                },
                                onPauseClick = {
                                    viewModel.pauseDownload(task.id)
                                },
                                onResumeClick = {
                                    viewModel.resumeDownload(task.id)
                                },
                                onDeleteClick = {
                                    viewModel.cancelDownload(task.id)
                                },
                                onShareClick = {},
                                onRetryClick = {
                                    viewModel.retryDownload(task.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(tab: DownloadTab) {
    when (tab) {
        DownloadTab.ALL -> {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.empty_all_title),
                description = stringResource(id = R.string.empty_all_desc)
            )
        }
        DownloadTab.DOWNLOADING -> {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.empty_downloading_title),
                description = stringResource(id = R.string.empty_downloading_desc)
            )
        }
        DownloadTab.COMPLETED -> {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.empty_completed_title),
                description = stringResource(id = R.string.empty_completed_desc)
            )
        }
        DownloadTab.FAILED -> {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.empty_failed_title),
                description = stringResource(id = R.string.empty_failed_desc)
            )
        }
    }
}

@Preview
@Composable
private fun DownloadsScreenPreview() {
    DownloadManagerTheme {
        DownloadsScreen()
    }
}
