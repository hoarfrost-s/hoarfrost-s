package com.downloadmanager.app.ui.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.downloadmanager.app.R
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun MultiSelectBar(
    selectedCount: Int,
    totalCount: Int,
    canStart: Boolean,
    canPause: Boolean,
    canShare: Boolean,
    onSelectAll: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedCount == totalCount && totalCount > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = R.string.selected_count,
                        selectedCount
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onSelectAll) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isAllSelected) {
                                androidx.compose.ui.res.stringResource(id = R.string.content_desc_unselect_all)
                            } else {
                                androidx.compose.ui.res.stringResource(id = R.string.content_desc_select_all)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MultiSelectActionItem(
                    icon = Icons.Default.PlayArrow,
                    label = androidx.compose.ui.res.stringResource(id = R.string.action_start),
                    enabled = canStart,
                    onClick = onStart
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Pause,
                    label = androidx.compose.ui.res.stringResource(id = R.string.action_pause),
                    enabled = canPause,
                    onClick = onPause
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Delete,
                    label = androidx.compose.ui.res.stringResource(id = R.string.action_delete),
                    enabled = selectedCount > 0,
                    onClick = onDelete
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Share,
                    label = androidx.compose.ui.res.stringResource(id = R.string.action_share),
                    enabled = canShare,
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun MultiSelectActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val clickableModifier = if (enabled) {
        Modifier.clickable(
            onClick = onClick,
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        )
    } else {
        Modifier
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .width(64.dp)
            .then(clickableModifier)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Preview
@Composable
private fun MultiSelectBarPreview() {
    DownloadManagerTheme {
        MultiSelectBar(
            selectedCount = 3,
            totalCount = 10,
            canStart = true,
            canPause = true,
            canShare = false,
            onSelectAll = {},
            onStart = {},
            onPause = {},
            onDelete = {},
            onShare = {}
        )
    }
}

@Preview
@Composable
private fun MultiSelectBarAllSelectedPreview() {
    DownloadManagerTheme {
        MultiSelectBar(
            selectedCount = 5,
            totalCount = 5,
            canStart = false,
            canPause = true,
            canShare = true,
            onSelectAll = {},
            onStart = {},
            onPause = {},
            onDelete = {},
            onShare = {}
        )
    }
}
