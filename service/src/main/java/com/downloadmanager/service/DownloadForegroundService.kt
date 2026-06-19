package com.downloadmanager.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.downloadmanager.engine.TaskScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var taskScheduler: TaskScheduler

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0,
                    packageManager.getLaunchIntentForPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val pauseIntent = PendingIntent.getService(
                    this, 1,
                    Intent(this, DownloadForegroundService::class.java).apply { action = ACTION_PAUSE_ALL },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val stopIntent = PendingIntent.getService(
                    this, 2,
                    Intent(this, DownloadForegroundService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = notificationHelper.buildProgressNotification(
                    activeCount = taskScheduler.getActiveCount(),
                    totalSpeed = 0,
                    overallProgress = 0,
                    pendingIntent = pendingIntent,
                    pauseIntent = pauseIntent,
                    stopIntent = stopIntent
                ).build()

                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
            ACTION_PAUSE_ALL -> {
                kotlinx.coroutines.runBlocking {
                    taskScheduler.pauseAll()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.downloadmanager.action.START"
        const val ACTION_PAUSE_ALL = "com.downloadmanager.action.PAUSE_ALL"
        const val ACTION_STOP = "com.downloadmanager.action.STOP"
    }
}