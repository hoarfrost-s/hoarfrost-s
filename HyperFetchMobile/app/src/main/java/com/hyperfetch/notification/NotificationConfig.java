package com.hyperfetch.notification;

import android.app.Notification;

/**
 * 通知配置类
 * 用于配置通知的显示属性
 */
public class NotificationConfig {

    /**
     * 通知渠道名称
     */
    private String channelName = "下载服务";

    /**
     * 通知渠道描述
     */
    private String channelDescription = "用于显示下载任务进度和状态的通知渠道";

    /**
     * 通知图标资源ID
     */
    private int notificationIcon = android.R.drawable.stat_sys_download;

    /**
     * 是否显示进度条
     */
    private boolean showProgress = true;

    /**
     * 是否常驻通知
     */
    private boolean ongoingNotification = true;

    /**
     * 是否显示角标
     */
    private boolean showBadge = false;

    /**
     * 前台服务通知标题
     */
    private String foregroundTitle = "HyperFetch";

    /**
     * 前台服务通知内容
     */
    private String foregroundContent = "下载服务运行中";

    /**
     * 是否启用完成通知
     */
    private boolean enableCompleteNotification = true;

    /**
     * 是否启用错误通知
     */
    private boolean enableErrorNotification = true;

    /**
     * 是否启用进度通知
     */
    private boolean enableProgressNotification = true;

    /**
     * 默认构造函数
     */
    public NotificationConfig() {
    }

    // Getter 和 Setter 方法

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelDescription() {
        return channelDescription;
    }

    public void setChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
    }

    public int getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(int notificationIcon) {
        this.notificationIcon = notificationIcon;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public boolean isOngoingNotification() {
        return ongoingNotification;
    }

    public void setOngoingNotification(boolean ongoingNotification) {
        this.ongoingNotification = ongoingNotification;
    }

    public boolean isShowBadge() {
        return showBadge;
    }

    public void setShowBadge(boolean showBadge) {
        this.showBadge = showBadge;
    }

    public String getForegroundTitle() {
        return foregroundTitle;
    }

    public void setForegroundTitle(String foregroundTitle) {
        this.foregroundTitle = foregroundTitle;
    }

    public String getForegroundContent() {
        return foregroundContent;
    }

    public void setForegroundContent(String foregroundContent) {
        this.foregroundContent = foregroundContent;
    }

    public boolean isEnableCompleteNotification() {
        return enableCompleteNotification;
    }

    public void setEnableCompleteNotification(boolean enableCompleteNotification) {
        this.enableCompleteNotification = enableCompleteNotification;
    }

    public boolean isEnableErrorNotification() {
        return enableErrorNotification;
    }

    public void setEnableErrorNotification(boolean enableErrorNotification) {
        this.enableErrorNotification = enableErrorNotification;
    }

    public boolean isEnableProgressNotification() {
        return enableProgressNotification;
    }

    public void setEnableProgressNotification(boolean enableProgressNotification) {
        this.enableProgressNotification = enableProgressNotification;
    }

    @Override
    public String toString() {
        return "NotificationConfig{" +
                "channelName='" + channelName + '\'' +
                ", channelDescription='" + channelDescription + '\'' +
                ", notificationIcon=" + notificationIcon +
                ", showProgress=" + showProgress +
                ", ongoingNotification=" + ongoingNotification +
                ", showBadge=" + showBadge +
                ", foregroundTitle='" + foregroundTitle + '\'' +
                ", foregroundContent='" + foregroundContent + '\'' +
                ", enableCompleteNotification=" + enableCompleteNotification +
                ", enableErrorNotification=" + enableErrorNotification +
                ", enableProgressNotification=" + enableProgressNotification +
                '}';
    }
}