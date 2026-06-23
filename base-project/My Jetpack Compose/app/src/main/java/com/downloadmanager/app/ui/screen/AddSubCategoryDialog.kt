package com.downloadmanager.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun AddSubCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: (extensions: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var extensionsText by remember { mutableStateOf("") }

    val isValid = extensionsText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加后缀名")
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "为「$categoryName」添加后缀名，多个用逗号分隔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = extensionsText,
                    onValueChange = { extensionsText = it },
                    placeholder = { Text("mp4, avi, mkv") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "提示：不需要输入点号，系统会自动添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        onConfirm(extensions)
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

@Preview
@Composable
private fun AddSubCategoryDialogPreview() {
    DownloadManagerTheme {
        AddSubCategoryDialog(
            categoryName = "视频",
            onDismiss = {},
            onConfirm = {}
        )
    }
}
