package com.downloadmanager.common

import java.util.Locale

object FormatUtils {
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "未知"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${bytes}B"
        else String.format(Locale.US, "%.1f%s", size, units[unitIndex])
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "0 B/s"
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "--:--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    fun formatEta(remainingBytes: Long, speedBytesPerSec: Long): String {
        if (speedBytesPerSec <= 0 || remainingBytes <= 0) return "--:--"
        return formatDuration(remainingBytes / speedBytesPerSec)
    }
}