package com.downloadmanager.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.downloadmanager.common.SegmentProgress

@Composable
fun SegmentedProgressBar(
    segments: List<SegmentProgress>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    val totalBytes = segments.sumOf { it.totalBytes }
    if (totalBytes == 0L) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { segment ->
            val progress by animateFloatAsState(
                targetValue = if (totalBytes > 0) segment.downloadedBytes.toFloat() / totalBytes else 0f,
                animationSpec = tween(durationMillis = 300)
            )
            val weight = if (totalBytes > 0) segment.totalBytes.toFloat() / totalBytes else 1f / segments.size

            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            segment.downloadedBytes >= segment.totalBytes -> Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                            segment.downloadedBytes > 0 -> Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                            else -> Color(0xFFE0E0E0)
                        }
                    )
            )
        }
    }
}