package com.downloadmanager.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.category.SubCategoryInfo
import com.downloadmanager.app.ui.component.AppTopBar
import com.downloadmanager.app.ui.component.ConfirmDialog
import com.downloadmanager.app.ui.component.EmptyState
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.viewmodel.CategoryUiState
import com.downloadmanager.app.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    val currentCategory = uiState.categories.find { it.id == uiState.currentCategoryId }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = "分类管理",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.showAddCategoryDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加分类"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
        } else if (uiState.categories.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Category,
                title = "暂无分类",
                description = "点击右上角按钮添加你的第一个分类",
                modifier = Modifier.padding(innerPadding),
                actionText = "添加分类",
                onActionClick = { viewModel.showAddCategoryDialog(true) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.categories,
                    key = { it.id }
                ) { category ->
                    CategoryGroupItem(
                        category = category,
                        isExpanded = uiState.expandedCategoryId == category.id,
                        onToggleExpand = { viewModel.toggleCategoryExpand(category.id) },
                        onAddSubCategory = {
                            viewModel.showAddSubCategoryDialog(true, category.id)
                        },
                        onRemoveSubCategory = { subCategoryId ->
                            viewModel.showDeleteSubCategoryConfirmDialog(true, subCategoryId)
                        },
                        onRename = {
                            viewModel.showRenameDialog(true, category.id)
                        },
                        onDelete = {
                            viewModel.showDeleteConfirmDialog(true, category.id)
                        }
                    )
                }
            }
        }

        if (uiState.showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { viewModel.showAddCategoryDialog(false) },
                onConfirm = { name, iconRes, extensions ->
                    viewModel.addCategory(name, iconRes, extensions)
                }
            )
        }

        if (uiState.showAddSubCategoryDialog && currentCategory != null) {
            AddSubCategoryDialog(
                categoryName = currentCategory.name,
                onDismiss = {
                    viewModel.showAddSubCategoryDialog(false, "")
                },
                onConfirm = { extensions ->
                    uiState.currentCategoryId?.let { categoryId ->
                        viewModel.addSubCategories(categoryId, extensions)
                    }
                }
            )
        }

        if (uiState.showRenameDialog && currentCategory != null) {
            RenameCategoryDialog(
                currentName = currentCategory.name,
                onDismiss = {
                    viewModel.showRenameDialog(false, "")
                },
                onConfirm = { newName ->
                    uiState.currentCategoryId?.let { categoryId ->
                        viewModel.renameCategory(categoryId, newName)
                    }
                }
            )
        }

        if (uiState.showDeleteConfirmDialog && currentCategory != null) {
            ConfirmDialog(
                title = "删除分类",
                message = "确定要删除「${currentCategory.name}」吗？该分类下的所有后缀名也会被删除。",
                confirmText = "删除",
                onConfirm = {
                    uiState.currentCategoryId?.let { categoryId ->
                        viewModel.removeCategory(categoryId)
                    }
                },
                onDismiss = {
                    viewModel.showDeleteConfirmDialog(false, "")
                },
                isConfirmDanger = true
            )
        }

        if (uiState.showDeleteSubCategoryConfirmDialog) {
            ConfirmDialog(
                title = "删除后缀名",
                message = "确定要删除这个后缀名吗？",
                confirmText = "删除",
                onConfirm = {
                    uiState.currentSubCategoryId?.let { subCategoryId ->
                        viewModel.removeSubCategory(subCategoryId)
                    }
                    viewModel.showDeleteSubCategoryConfirmDialog(false, "")
                },
                onDismiss = {
                    viewModel.showDeleteSubCategoryConfirmDialog(false, "")
                },
                isConfirmDanger = true
            )
        }
    }
}

@Preview
@Composable
private fun CategoryManageScreenPreview() {
    DownloadManagerTheme {
        val mockCategories = listOf(
            CategoryInfo(
                id = "1",
                name = "视频",
                iconRes = "video",
                sortOrder = 0,
                isDefault = true,
                subCategories = listOf(
                    SubCategoryInfo("1", "mp4", "1", 0),
                    SubCategoryInfo("2", "mkv", "1", 1),
                    SubCategoryInfo("3", "avi", "1", 2)
                )
            ),
            CategoryInfo(
                id = "2",
                name = "音乐",
                iconRes = "audio",
                sortOrder = 1,
                isDefault = true,
                subCategories = listOf(
                    SubCategoryInfo("4", "mp3", "2", 0),
                    SubCategoryInfo("5", "flac", "2", 1)
                )
            ),
            CategoryInfo(
                id = "3",
                name = "自定义分类",
                iconRes = "folder",
                sortOrder = 2,
                isDefault = false,
                subCategories = emptyList()
            )
        )
        val mockState = CategoryUiState(
            categories = mockCategories,
            expandedCategoryId = "1",
            isLoading = false
        )
        CategoryManageScreenPreviewContent(uiState = mockState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManageScreenPreviewContent(
    uiState: CategoryUiState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "分类管理",
                showBackButton = true,
                onBackClick = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加分类"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = uiState.categories,
                key = { it.id }
            ) { category ->
                CategoryGroupItem(
                    category = category,
                    isExpanded = uiState.expandedCategoryId == category.id,
                    onToggleExpand = {},
                    onAddSubCategory = {},
                    onRemoveSubCategory = {},
                    onRename = {},
                    onDelete = {}
                )
            }
        }
    }
}
