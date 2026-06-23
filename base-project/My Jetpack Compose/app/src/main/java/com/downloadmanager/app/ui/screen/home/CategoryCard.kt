package com.downloadmanager.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun CategoryCard(
    category: CategoryInfo,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(120.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = getCategoryIcon(category.name),
                contentDescription = category.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "$count 个文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "视频", "video", "电影", "movie" -> Icons.Default.VideoFile
        "音乐", "music", "音频", "audio" -> Icons.Default.MusicNote
        "图片", "image", "照片", "photo" -> Icons.Default.Image
        "文档", "document", "文件", "file" -> Icons.Default.Description
        "软件", "software", "应用", "app", "apk" -> Icons.Default.Android
        "压缩包", "zip", "archive", "压缩" -> Icons.Default.Folder
        else -> Icons.Default.InsertDriveFile
    }
}

@Preview
@Composable
private fun CategoryCardPreview() {
    DownloadManagerTheme {
        CategoryCard(
            category = CategoryInfo(
                id = "1",
                name = "视频",
                sortOrder = 0,
                isDefault = true
            ),
            count = 25,
            onClick = {}
        )
    }
}
