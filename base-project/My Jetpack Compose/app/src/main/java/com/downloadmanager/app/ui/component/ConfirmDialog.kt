package com.downloadmanager.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "取消",
    isConfirmDanger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isConfirmDanger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    DownloadManagerTheme {
        ConfirmDialog(
            title = "确认删除",
            message = "确定要删除这个分类吗？",
            confirmText = "删除",
            onConfirm = {},
            onDismiss = {},
            isConfirmDanger = true
        )
    }
}
