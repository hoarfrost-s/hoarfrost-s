package com.downloadmanager.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.util.FileUtils

@Composable
fun AddDownloadDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, fileName: String?, threadCount: Int) -> Unit,
    initialUrl: String? = null,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var fileName by remember { mutableStateOf("") }
    var threadCount by remember { mutableIntStateOf(3) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isNotEmpty() && fileName.isEmpty()) {
            fileName = FileUtils.getFileNameFromUrl(url)
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加下载")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("下载链接") },
                    placeholder = { Text("请输入下载链接") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名 (可选)") },
                    placeholder = { Text("自动从链接提取") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "线程数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$threadCount 线程",
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            (1..9).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count 线程") },
                                    onClick = {
                                        threadCount = count
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(
                            url.trim(),
                            fileName.takeIf { it.isNotBlank() },
                            threadCount
                        )
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview
@Composable
private fun AddDownloadDialogPreview() {
    DownloadManagerTheme {
        AddDownloadDialog(
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
