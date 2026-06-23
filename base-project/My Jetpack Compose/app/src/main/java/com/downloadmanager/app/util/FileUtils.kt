package com.downloadmanager.app.util

import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    fun getFileNameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val fileName = path.substringAfterLast('/')
        return if (fileName.isEmpty() || fileName == path) {
            "download_${System.currentTimeMillis()}"
        } else {
            fileName
        }
    }

    fun getExtension(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\')
        if (name.isEmpty() || !name.contains('.')) {
            return ""
        }
        return name.substringAfterLast('.').lowercase()
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val index = digitGroups.coerceAtMost(units.size - 1)
        val value = bytes / 1024.0.pow(index.toDouble())
        val format = if (index == 0) "0" else "0.##"
        return DecimalFormat(format).format(value) + " " + units[index]
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return if (bytesPerSecond <= 0) {
            "0 B/s"
        } else {
            formatFileSize(bytesPerSecond) + "/s"
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "--:--"
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
}
