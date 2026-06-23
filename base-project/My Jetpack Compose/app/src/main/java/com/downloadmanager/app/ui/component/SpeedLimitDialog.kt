package com.downloadmanager.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun SpeedLimitDialog(
    currentSpeedKb: Long = 0L,
    onConfirm: (Long) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var speedInput by remember(currentSpeedKb) {
        mutableStateOf(if (currentSpeedKb > 0) (currentSpeedKb / 1024).toString() else "")
    }

    val presetSpeeds = listOf(
        "不限速" to 0L,
        "128 KB/s" to 128L * 1024,
        "256 KB/s" to 256L * 1024,
        "512 KB/s" to 512L * 1024,
        "1 MB/s" to 1024L * 1024,
        "2 MB/s" to 2048L * 1024,
        "5 MB/s" to 5120L * 1024
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "下载速度限制")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it.filter { c -> c.isDigit() } },
                    label = { Text("速度 (KB/s)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "快捷选择",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetSpeeds.forEach { (label, speed) ->
                        AssistChip(
                            onClick = {
                                speedInput = if (speed > 0) (speed / 1024).toString() else ""
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val speedKb = speedInput.toLongOrNull() ?: 0L
                    onConfirm(speedKb * 1024)
                }
            ) {
                Text("确认")
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
private fun SpeedLimitDialogPreview() {
    DownloadManagerTheme {
        SpeedLimitDialog(
            currentSpeedKb = 0L,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
