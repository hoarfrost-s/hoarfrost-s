package com.downloadmanager.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun ThemeDialog(
    currentTheme: String = "system",
    onConfirm: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var selectedTheme by remember(currentTheme) { mutableStateOf(currentTheme) }

    val themeOptions = listOf(
        Triple("system", "跟随系统", "根据系统设置自动切换", Icons.Default.BrightnessAuto),
        Triple("light", "亮色模式", "使用亮色主题", Icons.Default.LightMode),
        Triple("dark", "暗色模式", "使用暗色主题", Icons.Default.DarkMode)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "主题模式")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                themeOptions.forEach { (value, title, description, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selectedTheme == value,
                                onClick = { selectedTheme = value }
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTheme) }
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
private fun ThemeDialogPreview() {
    DownloadManagerTheme {
        ThemeDialog(
            currentTheme = "system",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
