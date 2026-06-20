package com.hyperfetch.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hyperfetch.R
import com.hyperfetch.ui.MainActivity

/**
 * 通知管理器
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_DOWNLOAD = "download_channel"
        const val CHANNEL_COMPLETED = "completed_channel"
        const val CHANNEL_ERROR = "error_channel"

        private const val NOTIFICATION_ID_BASE = 2000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_COMPLETED,
                    "下载完成",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "下载完成通知"
                },
                NotificationChannel(
                    CHANNEL_ERROR,
                    "下载错误",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "下载错误通知"
                }
            )
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    /**
     * 创建主应用意图
     */
    private fun createMainIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 显示下载进度通知
     */
    fun showProgressNotification(
        taskId: String,
        fileName: String,
        progress: Int,
        speed: String,
        eta: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle(fileName)
            .setContentText("$speed · $eta")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setContentIntent(createMainIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .build()

        val notificationId = (NOTIFICATION_ID_BASE + taskId.hashCode()).and(0x7FFFFFFF)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示下载完成通知
     */
    fun showCompletedNotification(
        taskId: String,
        fileName: String,
        filePath: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setContentTitle(context.getString(R.string.notification_completed))
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(createMainIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationId = (NOTIFICATION_ID_BASE + taskId.hashCode()).and(0x7FFFFFFF)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示下载错误通知
     */
    fun showErrorNotification(
        taskId: String,
        fileName: String,
        errorMessage: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR)
            .setContentTitle(context.getString(R.string.notification_failed))
            .setContentText("$fileName: $errorMessage")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(createMainIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = (NOTIFICATION_ID_BASE + taskId.hashCode()).and(0x7FFFFFFF)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification(taskId: String) {
        val notificationId = (NOTIFICATION_ID_BASE + taskId.hashCode()).and(0x7FFFFFFF)
        notificationManager.cancel(notificationId)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
