package com.downloadmanager.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun SectionHeader(
    title: String,
    onMoreClick: (() -> Unit)? = null,
    moreText: String = "查看更多",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (onMoreClick != null) {
            Text(
                text = moreText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onMoreClick)
            )
        }
    }
}

@Preview
@Composable
private fun SectionHeaderWithMorePreview() {
    DownloadManagerTheme {
        SectionHeader(
            title = "正在下载",
            onMoreClick = {}
        )
    }
}

@Preview
@Composable
private fun SectionHeaderWithoutMorePreview() {
    DownloadManagerTheme {
        SectionHeader(
            title = "已完成",
            onMoreClick = null
        )
    }
}
