package com.downloadmanager.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.ui.component.AddDownloadDialog
import com.downloadmanager.app.ui.component.AppTopBar
import com.downloadmanager.app.ui.component.ClipboardDialog
import com.downloadmanager.app.ui.component.EmptyState
import com.downloadmanager.app.ui.component.SectionHeader
import com.downloadmanager.app.ui.component.StatCard
import com.downloadmanager.app.ui.screen.home.CategoryCard
import com.downloadmanager.app.ui.screen.home.RecentTaskItem
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.ui.theme.LightPrimary
import com.downloadmanager.app.ui.theme.LightSecondary
import com.downloadmanager.app.ui.theme.LightTertiary
import com.downloadmanager.app.util.FileUtils
import com.downloadmanager.app.viewmodel.HomeViewModel
import com.downloadmanager.app.viewmodel.HomeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDownloads: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDownloads = onNavigateToDownloads,
        onAddClick = { viewModel.showAddDialog(true) },
        onDismissAddDialog = { viewModel.showAddDialog(false) },
        onConfirmAddDownload = { url, fileName, _ ->
            viewModel.addDownloadFromUi(url, fileName)
        },
        onDismissClipboardDialog = { viewModel.clearClipboardUrl() },
        onClipboardDownload = {
            val url = uiState.clipboardUrl
            if (url != null) {
                viewModel.clearClipboardUrl()
                viewModel.showAddDialog(true)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: (String?) -> Unit,
    onAddClick: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onConfirmAddDownload: (String, String?, Int) -> Unit,
    onDismissClipboardDialog: () -> Unit,
    onClipboardDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = "下载管理器",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("新建下载") }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    StatCardsSection(
                        totalDownloadedSize = uiState.totalDownloadedSize,
                        todayDownloadCount = uiState.todayDownloadCount,
                        downloadingCount = uiState.recentTasks.count {
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING || it.status == DownloadStatus.WAITING
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SectionHeader(
                        title = "分类",
                        onMoreClick = { onNavigateToDownloads(null) },
                        moreText = "查看更多"
                    )
                }

                item {
                    CategoryListSection(
                        categories = uiState.categories,
                        onCategoryClick = { categoryId ->
                            onNavigateToDownloads(categoryId)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SectionHeader(
                        title = "最近下载",
                        onMoreClick = { onNavigateToDownloads(null) },
                        moreText = "查看全部"
                    )
                }

                if (uiState.recentTasks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Download,
                            title = "暂无下载",
                            description = "点击右下角按钮开始你的第一个下载",
                            modifier = Modifier.height(240.dp)
                        )
                    }
                } else {
                    items(uiState.recentTasks, key = { it.id }) { task ->
                        RecentTaskSectionItem(
                            task = task,
                            categoryName = uiState.categories.find { it.id == task.categoryId }?.name
                        )
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            AddDownloadDialog(
                onDismiss = onDismissAddDialog,
                onConfirm = { url, fileName, threadCount ->
                    onConfirmAddDownload(url, fileName, threadCount)
                },
                initialUrl = uiState.clipboardUrl
            )
        }

        if (uiState.clipboardUrl != null && !uiState.showAddDialog) {
            ClipboardDialog(
                url = uiState.clipboardUrl,
                onDismiss = onDismissClipboardDialog,
                onDownload = onClipboardDownload
            )
        }
    }
}

@Composable
private fun StatCardsSection(
    totalDownloadedSize: Long,
    todayDownloadCount: Int,
    downloadingCount: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                icon = Icons.Default.DownloadDone,
                value = FileUtils.formatFileSize(totalDownloadedSize),
                label = "总下载量",
                gradientColors = listOf(LightPrimary, LightSecondary),
                modifier = Modifier
            )
        }
        item {
            StatCard(
                icon = Icons.Default.Today,
                value = todayDownloadCount.toString(),
                label = "今日下载",
                gradientColors = listOf(LightSecondary, LightTertiary),
                modifier = Modifier
            )
        }
        item {
            StatCard(
                icon = Icons.Default.Download,
                value = downloadingCount.toString(),
                label = "下载中",
                gradientColors = listOf(LightTertiary, LightPrimary),
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun CategoryListSection(
    categories: List<CategoryInfo>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryCard(
                category = category,
                count = (category.id.hashCode() % 20).coerceAtLeast(0),
                onClick = { onCategoryClick(category.id) }
            )
        }
    }
}

@Composable
private fun RecentTaskSectionItem(
    task: DownloadTaskEntity,
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        RecentTaskItem(
            task = task,
            categoryName = categoryName,
            speed = if (task.status == DownloadStatus.DOWNLOADING) 1024 * 512 else 0L
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    DownloadManagerTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                categories = listOf(
                    CategoryInfo(id = "1", name = "视频", sortOrder = 0, isDefault = true),
                    CategoryInfo(id = "2", name = "音乐", sortOrder = 1, isDefault = false),
                    CategoryInfo(id = "3", name = "图片", sortOrder = 2, isDefault = false),
                    CategoryInfo(id = "4", name = "文档", sortOrder = 3, isDefault = false),
                    CategoryInfo(id = "5", name = "软件", sortOrder = 4, isDefault = false)
                ),
                recentTasks = listOf(
                    DownloadTaskEntity(
                        id = "1",
                        url = "https://example.com/file1.zip",
                        fileName = "example_file_1.zip",
                        originalName = "file1.zip",
                        totalSize = 1024 * 1024 * 100,
                        downloadedSize = 1024 * 1024 * 65,
                        status = DownloadStatus.DOWNLOADING,
                        categoryId = "1",
                        savePath = "/downloads",
                        tempPath = "/downloads/temp",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    DownloadTaskEntity(
                        id = "2",
                        url = "https://example.com/video.mp4",
                        fileName = "sample_video.mp4",
                        originalName = "video.mp4",
                        totalSize = 1024 * 1024 * 500,
                        downloadedSize = 1024 * 1024 * 200,
                        status = DownloadStatus.PAUSED,
                        categoryId = "1",
                        savePath = "/downloads",
                        tempPath = "/downloads/temp",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    DownloadTaskEntity(
                        id = "3",
                        url = "https://example.com/music.mp3",
                        fileName = "song.mp3",
                        originalName = "music.mp3",
                        totalSize = 1024 * 1024 * 5,
                        downloadedSize = 1024 * 1024 * 5,
                        status = DownloadStatus.COMPLETED,
                        categoryId = "2",
                        savePath = "/downloads",
                        tempPath = "/downloads/temp",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                ),
                totalDownloadedSize = 1024 * 1024 * 1024 * 2,
                todayDownloadCount = 5,
                isLoading = false,
                showAddDialog = false,
                clipboardUrl = null
            ),
            onNavigateToSettings = {},
            onNavigateToDownloads = {},
            onAddClick = {},
            onDismissAddDialog = {},
            onConfirmAddDownload = { _, _, _ -> },
            onDismissClipboardDialog = {},
            onClipboardDownload = {}
        )
    }
}
