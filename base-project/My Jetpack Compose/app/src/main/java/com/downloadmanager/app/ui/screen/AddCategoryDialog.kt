package com.downloadmanager.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

data class IconOption(
    val key: String,
    val icon: ImageVector,
    val label: String
)

val defaultIconOptions = listOf(
    IconOption("folder", Icons.Default.Folder, "文件夹"),
    IconOption("video", Icons.Default.PlayArrow, "视频"),
    IconOption("audio", Icons.Default.MusicNote, "音乐"),
    IconOption("image", Icons.Default.Image, "图片"),
    IconOption("document", Icons.Default.TextSnippet, "文档")
)

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconRes: String, extensions: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("folder") }
    var extensionsText by remember { mutableStateOf("") }

    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加分类")
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Text(
                        text = "选择图标",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        defaultIconOptions.forEach { option ->
                            IconSelectorItem(
                                icon = option.icon,
                                label = option.label,
                                selected = selectedIcon == option.key,
                                onClick = { selectedIcon = option.key }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "后缀名（多个用逗号分隔）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = extensionsText,
                        onValueChange = { extensionsText = it },
                        placeholder = { Text("mp4, avi, mkv") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val extensions = extensionsText
                            .split(",", " ", "，")
                            .map { it.trim().lowercase().removePrefix(".") }
                            .filter { it.isNotBlank() }
                        onConfirm(name.trim(), selectedIcon, extensions)
                    }
                },
                enabled = isValid
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun IconSelectorItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier
                }
            )
            .padding(4.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = backgroundColor,
            contentColor = contentColor,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Preview
@Composable
private fun AddCategoryDialogPreview() {
    DownloadManagerTheme {
        AddCategoryDialog(
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
