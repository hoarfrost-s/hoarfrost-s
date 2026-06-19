package com.downloadmanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.downloadmanager.common.FormatUtils

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE_ALL = "com.downloadmanager.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.downloadmanager.action.RESUME_ALL"
        const val ACTION_STOP = "com.downloadmanager.action.STOP"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "下载任务进度通知"
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        activeCount: Int,
        totalSpeed: Long,
        overallProgress: Int,
        pendingIntent: PendingIntent,
        pauseIntent: PendingIntent,
        stopIntent: PendingIntent
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("下载管理器 — ${activeCount} 个任务进行中")
            .setContentText("总速度: ${FormatUtils.formatSpeed(totalSpeed)}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, overallProgress, false)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "全部暂停", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "停止", stopIntent)
    }
}