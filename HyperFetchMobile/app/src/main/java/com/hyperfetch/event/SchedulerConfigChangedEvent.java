package com.hyperfetch.event;

/**
 * 调度器配置变更事件类
 * 用于通知调度器配置已更新
 */
public class SchedulerConfigChangedEvent {

    /**
     * 最大并发任务数
     */
    private int maxConcurrentTasks;

    /**
     * 全局下载速度限制（字节/秒，0表示不限制）
     */
    private long globalSpeedLimit;

    /**
     * 是否启用自动重试
     */
    private boolean autoRetryEnabled;

    /**
     * 最大重试次数
     */
    private int maxRetryCount;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public SchedulerConfigChangedEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param maxConcurrentTasks 最大并发任务数
     */
    public SchedulerConfigChangedEvent(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 完整参数的构造函数
     *
     * @param maxConcurrentTasks 最大并发任务数
     * @param globalSpeedLimit   全局速度限制
     * @param autoRetryEnabled   是否启用自动重试
     * @param maxRetryCount      最大重试次数
     */
    public SchedulerConfigChangedEvent(int maxConcurrentTasks, long globalSpeedLimit,
                                       boolean autoRetryEnabled, int maxRetryCount) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.globalSpeedLimit = globalSpeedLimit;
        this.autoRetryEnabled = autoRetryEnabled;
        this.maxRetryCount = maxRetryCount;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter 和 Setter 方法

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public long getGlobalSpeedLimit() {
        return globalSpeedLimit;
    }

    public void setGlobalSpeedLimit(long globalSpeedLimit) {
        this.globalSpeedLimit = globalSpeedLimit;
    }

    public boolean isAutoRetryEnabled() {
        return autoRetryEnabled;
    }

    public void setAutoRetryEnabled(boolean autoRetryEnabled) {
        this.autoRetryEnabled = autoRetryEnabled;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SchedulerConfigChangedEvent{" +
                "maxConcurrentTasks=" + maxConcurrentTasks +
                ", globalSpeedLimit=" + globalSpeedLimit +
                ", autoRetryEnabled=" + autoRetryEnabled +
                ", maxRetryCount=" + maxRetryCount +
                ", timestamp=" + timestamp +
                '}';
    }
}