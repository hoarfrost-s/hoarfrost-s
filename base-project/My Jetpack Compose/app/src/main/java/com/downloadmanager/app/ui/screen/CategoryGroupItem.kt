package com.downloadmanager.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.category.SubCategoryInfo
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryGroupItem(
    category: CategoryInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddSubCategory: () -> Unit,
    onRemoveSubCategory: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.iconRes),
                        contentDescription = category.name,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (category.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${category.subCategories.size} 个后缀名",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        modifier = Modifier.rotate(rotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    if (category.subCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无后缀名",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            category.subCategories.forEach { subCategory ->
                                SubCategoryChip(
                                    extension = subCategory.extension,
                                    onRemove = { onRemoveSubCategory(subCategory.id) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddSubCategory,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "添加后缀名")
                        }
                        if (!category.isDefault) {
                            OutlinedButton(onClick = onRename) {
                                Text(text = "重命名")
                            }
                            OutlinedButton(onClick = onDelete) {
                                Text(
                                    text = "删除",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCategoryChip(
    extension: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = ".$extension",
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier,
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

fun getCategoryIcon(iconRes: String?): ImageVector {
    return when (iconRes) {
        "video" -> Icons.Default.PlayArrow
        "audio" -> Icons.Default.MusicNote
        "image" -> Icons.Default.Image
        "document" -> Icons.Default.TextSnippet
        "archive" -> Icons.Default.Folder
        "settings" -> Icons.Default.Settings
        else -> Icons.Default.Folder
    }
}

@Preview
@Composable
private fun CategoryGroupItemExpandedPreview() {
    DownloadManagerTheme {
        CategoryGroupItem(
            category = CategoryInfo(
                id = "1",
                name = "视频",
                iconRes = "video",
                sortOrder = 0,
                isDefault = true,
                subCategories = listOf(
                    SubCategoryInfo("1", "mp4", "1", 0),
                    SubCategoryInfo("2", "mkv", "1", 1),
                    SubCategoryInfo("3", "avi", "1", 2),
                    SubCategoryInfo("4", "mov", "1", 3),
                    SubCategoryInfo("5", "wmv", "1", 4)
                )
            ),
            isExpanded = true,
            onToggleExpand = {},
            onAddSubCategory = {},
            onRemoveSubCategory = {},
            onRename = {},
            onDelete = {}
        )
    }
}

@Preview
@Composable
private fun CategoryGroupItemCollapsedPreview() {
    DownloadManagerTheme {
        CategoryGroupItem(
            category = CategoryInfo(
                id = "2",
                name = "自定义分类",
                iconRes = "folder",
                sortOrder = 1,
                isDefault = false,
                subCategories = listOf(
                    SubCategoryInfo("1", "exe", "2", 0)
                )
            ),
            isExpanded = false,
            onToggleExpand = {},
            onAddSubCategory = {},
            onRemoveSubCategory = {},
            onRename = {},
            onDelete = {}
        )
    }
}

@Preview
@Composable
private fun CategoryGroupItemEmptyPreview() {
    DownloadManagerTheme {
        CategoryGroupItem(
            category = CategoryInfo(
                id = "3",
                name = "空分类",
                iconRes = "folder",
                sortOrder = 2,
                isDefault = false,
                subCategories = emptyList()
            ),
            isExpanded = true,
            onToggleExpand = {},
            onAddSubCategory = {},
            onRemoveSubCategory = {},
            onRename = {},
            onDelete = {}
        )
    }
}
