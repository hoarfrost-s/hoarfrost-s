package com.downloadmanager.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    FAILED,
    COMPLETED
}

@Composable
fun DownloadProgressBar(
    progress: Float,
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val progressColor = when (status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f),
                color = progressColor,
                trackColor = trackColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun DownloadProgressBarDownloadingPreview() {
    DownloadManagerTheme {
        DownloadProgressBar(
            progress = 0.65f,
            status = DownloadStatus.DOWNLOADING
        )
    }
}

@Preview
@Composable
private fun DownloadProgressBarPausedPreview() {
    DownloadManagerTheme {
        DownloadProgressBar(
            progress = 0.3f,
            status = DownloadStatus.PAUSED
        )
    }
}

@Preview
@Composable
private fun DownloadProgressBarFailedPreview() {
    DownloadManagerTheme {
        DownloadProgressBar(
            progress = 0.45f,
            status = DownloadStatus.FAILED
        )
    }
}

@Preview
@Composable
private fun DownloadProgressBarCompletedPreview() {
    DownloadManagerTheme {
        DownloadProgressBar(
            progress = 1f,
            status = DownloadStatus.COMPLETED
        )
    }
}
