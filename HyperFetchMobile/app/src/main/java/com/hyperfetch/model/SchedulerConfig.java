package com.hyperfetch.model;

/**
 * 调度配置类
 * 用于配置下载调度器的参数
 */
public class SchedulerConfig {

    /**
     * 最大并发任务数，默认为3
     */
    private int maxConcurrentTasks = 3;

    /**
     * 是否启用多任务下载，默认为true
     */
    private boolean multiTaskEnabled = true;

    /**
     * 队列策略，默认为"priority"（优先级）
     */
    private String queueStrategy = "priority";

    /**
     * 默认构造函数
     */
    public SchedulerConfig() {
    }

    /**
     * 带参数的构造函数
     *
     * @param maxConcurrentTasks 最大并发任务数
     * @param multiTaskEnabled   是否启用多任务下载
     * @param queueStrategy      队列策略
     */
    public SchedulerConfig(int maxConcurrentTasks, boolean multiTaskEnabled, String queueStrategy) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.multiTaskEnabled = multiTaskEnabled;
        this.queueStrategy = queueStrategy;
    }

    // Getter 和 Setter 方法

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public boolean isMultiTaskEnabled() {
        return multiTaskEnabled;
    }

    public void setMultiTaskEnabled(boolean multiTaskEnabled) {
        this.multiTaskEnabled = multiTaskEnabled;
    }

    public String getQueueStrategy() {
        return queueStrategy;
    }

    public void setQueueStrategy(String queueStrategy) {
        this.queueStrategy = queueStrategy;
    }
}