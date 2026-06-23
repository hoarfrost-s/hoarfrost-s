package com.downloadmanager.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.downloadmanager.app.MainActivity
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.engine.DownloadProgress
import com.downloadmanager.app.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "下载通知"

        const val ACTION_PAUSE = "com.downloadmanager.app.action.PAUSE"
        const val ACTION_RESUME = "com.downloadmanager.app.action.RESUME"
        const val ACTION_CANCEL = "com.downloadmanager.app.action.CANCEL"
        const val ACTION_RETRY = "com.downloadmanager.app.action.RETRY"
        const val ACTION_OPEN = "com.downloadmanager.app.action.OPEN"
        const val ACTION_SHARE = "com.downloadmanager.app.action.SHARE"

        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "下载进度和状态通知"
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationId(taskId: String): Int {
        return taskId.hashCode()
    }

    fun buildDownloadingNotification(
        task: DownloadTaskEntity,
        progress: DownloadProgress
    ): NotificationCompat.Builder {
        val percent = if (progress.totalSize > 0) {
            ((progress.downloadedSize.toFloat() / progress.totalSize.toFloat()) * 100).toInt()
        } else {
            0
        }

        val speedText = FileUtils.formatSpeed(progress.speed)
        val sizeText = "${FileUtils.formatFileSize(progress.downloadedSize)} / ${FileUtils.formatFileSize(progress.totalSize)}"
        val contentText = "$percent% · $speedText · $sizeText"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(task.originalName)
            .setContentText(contentText)
            .setProgress(100, percent, progress.totalSize <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getContentPendingIntent(task.id))

        builder.addAction(
            android.R.drawable.ic_media_pause,
            "暂停",
            getActionPendingIntent(ACTION_PAUSE, task.id)
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "取消",
            getActionPendingIntent(ACTION_CANCEL, task.id)
        )

        return builder
    }

    fun buildPausedNotification(task: DownloadTaskEntity): NotificationCompat.Builder {
        val percent = if (task.totalSize > 0) {
            ((task.downloadedSize.toFloat() / task.totalSize.toFloat()) * 100).toInt()
        } else {
            0
        }

        val sizeText = "${FileUtils.formatFileSize(task.downloadedSize)} / ${FileUtils.formatFileSize(task.totalSize)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(task.originalName)
            .setContentText("已暂停 · $percent% · $sizeText")
            .setProgress(100, percent, false)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getContentPendingIntent(task.id))

        builder.addAction(
            android.R.drawable.ic_media_play,
            "继续",
            getActionPendingIntent(ACTION_RESUME, task.id)
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "取消",
            getActionPendingIntent(ACTION_CANCEL, task.id)
        )

        return builder
    }

    fun buildCompletedNotification(task: DownloadTaskEntity): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(task.originalName)
            .setContentText("下载完成")
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getOpenFilePendingIntent(task.id))

        builder.addAction(
            android.R.drawable.ic_menu_view,
            "打开",
            getActionPendingIntent(ACTION_OPEN, task.id)
        )
        builder.addAction(
            android.R.drawable.ic_menu_share,
            "分享",
            getActionPendingIntent(ACTION_SHARE, task.id)
        )

        return builder
    }

    fun buildFailedNotification(task: DownloadTaskEntity): NotificationCompat.Builder {
        val errorMessage = task.errorMessage ?: "未知错误"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(task.originalName)
            .setContentText("下载失败：$errorMessage")
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getContentPendingIntent(task.id))

        builder.addAction(
            android.R.drawable.ic_menu_rotate,
            "重试",
            getActionPendingIntent(ACTION_RETRY, task.id)
        )

        return builder
    }

    fun updateNotification(taskId: String, builder: NotificationCompat.Builder) {
        try {
            val notificationId = getNotificationId(taskId)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
        } catch (e: Exception) {
        }
    }

    fun cancelNotification(taskId: String) {
        try {
            val notificationId = getNotificationId(taskId)
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
        }
    }

    private fun getContentPendingIntent(taskId: String): PendingIntent? {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenFilePendingIntent(taskId: String): PendingIntent? {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            (taskId + "_open").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getActionPendingIntent(action: String, taskId: String): PendingIntent? {
        val isServiceAction = action == ACTION_PAUSE || action == ACTION_RESUME ||
                action == ACTION_CANCEL || action == ACTION_RETRY

        return if (isServiceAction) {
            getServiceActionPendingIntent(action, taskId)
        } else {
            getActivityActionPendingIntent(action, taskId)
        }
    }

    private fun getServiceActionPendingIntent(action: String, taskId: String): PendingIntent? {
        val intent = Intent(context, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getService(
            context,
            (taskId + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getActivityActionPendingIntent(action: String, taskId: String): PendingIntent? {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            (taskId + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
