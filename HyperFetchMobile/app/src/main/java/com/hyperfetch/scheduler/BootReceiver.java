package com.hyperfetch.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hyperfetch.service.DownloadService;
import com.hyperfetch.service.RecoveryManager;
import com.hyperfetch.service.ServiceActions;

/**
 * 开机启动接收器
 * 监听系统开机完成事件，自动恢复未完成的下载任务
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    /**
     * 接收广播事件
     *
     * @param context 应用上下文
     * @param intent  接收到的 Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "收到空 Intent，忽略");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "收到广播: " + action);

        // 处理开机完成事件
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON")) {
            handleBootCompleted(context);
        }
    }

    /**
     * 处理开机完成事件
     * 启动 DownloadService 恢复未完成的下载任务
     *
     * @param context 应用上下文
     */
    private void handleBootCompleted(Context context) {
        Log.d(TAG, "系统启动完成，开始恢复下载任务...");

        try {
            // 初始化 RecoveryManager 并执行立即恢复检查
            RecoveryManager recoveryManager = RecoveryManager.getInstance(context);
            recoveryManager.performImmediateRecovery();

            // 注册周期性恢复任务
            recoveryManager.registerPeriodicRecovery();

            Log.d(TAG, "下载恢复任务已启动");

        } catch (Exception e) {
            Log.e(TAG, "启动下载恢复失败", e);

            // 备用方案：直接启动 DownloadService
            startDownloadServiceFallback(context);
        }
    }

    /**
     * 备用方案：直接启动 DownloadService
     * 当 RecoveryManager 启动失败时使用
     *
     * @param context 应用上下文
     */
    private void startDownloadServiceFallback(Context context) {
        Log.d(TAG, "使用备用方案启动 DownloadService");

        Intent serviceIntent = new Intent(context, DownloadService.class);
        serviceIntent.setAction(ServiceActions.ACTION_RECOVER);

        // Android 8.0+ 需要使用 startForegroundService
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}