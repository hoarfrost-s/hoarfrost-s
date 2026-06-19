package com.hyperfetch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hyperfetch.service.DownloadService

/**
 * 开机广播接收器
 * 用于应用被系统回收后自动恢复下载任务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            // 启动下载服务
            val serviceIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
