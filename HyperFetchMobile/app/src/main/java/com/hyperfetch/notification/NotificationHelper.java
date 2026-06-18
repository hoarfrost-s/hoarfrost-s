package com.hyperfetch.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.hyperfetch.event.CompletedEvent;
import com.hyperfetch.event.ErrorEvent;
import com.hyperfetch.event.ProgressEvent;

/**
 * 通知管理类
 * 用于管理下载任务的通知显示
 */
public class NotificationHelper {

    /**
     * 通知渠道ID
     */
    private static final String CHANNEL_ID = "download_channel";

    /**
     * 前台服务通知ID
     */
    private static final int NOTIFICATION_ID = 1001;

    /**
     * 通知管理器
     */
    private NotificationManager notificationManager;

    /**
     * 通知配置
     */
    private NotificationConfig config;

    /**
     * 单例实例
     */
    private static volatile NotificationHelper instance;

    /**
     * 私有构造函数
     */
    private NotificationHelper() {
        this.config = new NotificationConfig();
    }

    /**
     * 获取单例实例
     *
     * @return NotificationHelper实例
     */
    public static NotificationHelper getInstance() {
        if (instance == null) {
            synchronized (NotificationHelper.class) {
                if (instance == null) {
                    instance = new NotificationHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 创建通知渠道
     * Android 8.0及以上版本需要
     *
     * @param context 上下文
     */
    public void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    config.getChannelName(),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(config.getChannelDescription());
            channel.setShowBadge(config.isShowBadge());

            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 构建前台服务通知
     *
     * @param context 上下文
     * @return 通知对象
     */
    public Notification buildForegroundNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(config.getForegroundTitle())
                .setContentText(config.getForegroundContent())
                .setSmallIcon(config.getNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false);

        if (config.isShowProgress()) {
            builder.setProgress(100, 0, true);
        }

        return builder.build();
    }

    /**
     * 更新进度通知
     *
     * @param context 上下文
     * @param event   进度事件
     */
    public void updateProgress(Context context, ProgressEvent event) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        String contentText = formatProgressText(event);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("下载中: " + event.getTaskId())
                .setContentText(contentText)
                .setSmallIcon(config.getNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (config.isShowProgress() && event.getTotalSize() > 0) {
            builder.setProgress(100, event.getProgress(), false);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * 显示完成通知
     *
     * @param context 上下文
     * @param event   完成事件
     */
    public void showComplete(Context context, CompletedEvent event) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        String contentText = event.isHasWarning()
                ? "下载完成（有警告）: " + event.getWarningMessage()
                : "文件已保存到: " + event.getFilePath();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("下载完成")
                .setContentText(contentText)
                .setSmallIcon(config.getNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(generateNotificationId(event.getTaskId()), builder.build());
    }

    /**
     * 显示错误通知
     *
     * @param context 上下文
     * @param event   错误事件
     */
    public void showError(Context context, ErrorEvent event) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        String contentText = event.isRetryable()
                ? "错误: " + event.getCause() + " (重试 " + event.getRetryCount() + " 次)"
                : "错误: " + event.getCause();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("下载失败")
                .setContentText(contentText)
                .setSmallIcon(config.getNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(generateNotificationId(event.getTaskId()), builder.build());
    }

    /**
     * 取消通知
     *
     * @param context 上下文
     * @param taskId  任务ID
     */
    public void cancelNotification(Context context, String taskId) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        notificationManager.cancel(generateNotificationId(taskId));
    }

    /**
     * 取消所有通知
     *
     * @param context 上下文
     */
    public void cancelAllNotifications(Context context) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        notificationManager.cancelAll();
    }

    /**
     * 格式化进度文本
     *
     * @param event 进度事件
     * @return 格式化后的文本
     */
    private String formatProgressText(ProgressEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatSize(event.getDownloaded()));
        sb.append(" / ");
        sb.append(formatSize(event.getTotalSize()));
        sb.append(" - ");
        sb.append(formatSpeed(event.getSpeed()));
        return sb.toString();
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 格式化下载速度
     *
     * @param bytesPerSecond 每秒字节数
     * @return 格式化后的速度字符串
     */
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        }
    }

    /**
     * 根据任务ID生成通知ID
     *
     * @param taskId 任务ID
     * @return 通知ID
     */
    private int generateNotificationId(String taskId) {
        return taskId != null ? taskId.hashCode() : NOTIFICATION_ID;
    }

    /**
     * 获取通知渠道ID
     *
     * @return 通知渠道ID
     */
    public static String getChannelId() {
        return CHANNEL_ID;
    }

    /**
     * 获取前台服务通知ID
     *
     * @return 前台服务通知ID
     */
    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    /**
     * 获取通知配置
     *
     * @return 通知配置
     */
    public NotificationConfig getConfig() {
        return config;
    }

    /**
     * 设置通知配置
     *
     * @param config 通知配置
     */
    public void setConfig(NotificationConfig config) {
        this.config = config;
    }
}