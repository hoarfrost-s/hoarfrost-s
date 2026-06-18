package com.hyperfetch.service;

import com.hyperfetch.model.Priority;

/**
 * 任务创建选项类
 * 用于配置新建下载任务的各项参数
 */
public class TaskOptions {

    /**
     * 保存路径
     */
    private String savePath;

    /**
     * 线程数，默认4
     */
    private int threadCount = 4;

    /**
     * 限速（字节/秒），默认无限制（0表示不限制）
     */
    private long speedLimit = 0;

    /**
     * 优先级，默认NORMAL
     */
    private Priority priority = Priority.NORMAL;

    /**
     * 是否立即开始下载，默认true
     */
    private boolean startImmediately = true;

    /**
     * 默认构造函数
     */
    public TaskOptions() {
    }

    /**
     * 带保存路径的构造函数
     *
     * @param savePath 文件保存路径
     */
    public TaskOptions(String savePath) {
        this.savePath = savePath;
    }

    /**
     * 全参数构造函数
     *
     * @param savePath         文件保存路径
     * @param threadCount      线程数
     * @param speedLimit       限速（字节/秒）
     * @param priority         优先级
     * @param startImmediately 是否立即开始
     */
    public TaskOptions(String savePath, int threadCount, long speedLimit,
                       Priority priority, boolean startImmediately) {
        this.savePath = savePath;
        this.threadCount = threadCount;
        this.speedLimit = speedLimit;
        this.priority = priority;
        this.startImmediately = startImmediately;
    }

    // ==================== Builder模式 ====================

    /**
     * 创建Builder实例
     *
     * @return Builder对象
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类，用于链式构建TaskOptions
     */
    public static class Builder {
        private String savePath;
        private int threadCount = 4;
        private long speedLimit = 0;
        private Priority priority = Priority.NORMAL;
        private boolean startImmediately = true;

        /**
         * 设置保存路径
         *
         * @param savePath 文件保存路径
         * @return Builder对象
         */
        public Builder savePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        /**
         * 设置线程数
         *
         * @param threadCount 线程数（1-9）
         * @return Builder对象
         */
        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        /**
         * 设置限速
         *
         * @param speedLimit 限速（字节/秒），0表示不限制
         * @return Builder对象
         */
        public Builder speedLimit(long speedLimit) {
            this.speedLimit = speedLimit;
            return this;
        }

        /**
         * 设置优先级
         *
         * @param priority 优先级
         * @return Builder对象
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置是否立即开始
         *
         * @param startImmediately 是否立即开始
         * @return Builder对象
         */
        public Builder startImmediately(boolean startImmediately) {
            this.startImmediately = startImmediately;
            return this;
        }

        /**
         * 构建TaskOptions实例
         *
         * @return TaskOptions对象
         */
        public TaskOptions build() {
            return new TaskOptions(savePath, threadCount, speedLimit,
                    priority, startImmediately);
        }
    }

    // ==================== Getter和Setter方法 ====================

    /**
     * 获取保存路径
     *
     * @return 保存路径
     */
    public String getSavePath() {
        return savePath;
    }

    /**
     * 设置保存路径
     *
     * @param savePath 保存路径
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /**
     * 获取线程数
     *
     * @return 线程数
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * 设置线程数
     *
     * @param threadCount 线程数（1-9）
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * 获取限速
     *
     * @return 限速（字节/秒），0表示不限制
     */
    public long getSpeedLimit() {
        return speedLimit;
    }

    /**
     * 设置限速
     *
     * @param speedLimit 限速（字节/秒），0表示不限制
     */
    public void setSpeedLimit(long speedLimit) {
        this.speedLimit = speedLimit;
    }

    /**
     * 获取优先级
     *
     * @return 优先级
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * 设置优先级
     *
     * @param priority 优先级
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * 获取是否立即开始
     *
     * @return 是否立即开始
     */
    public boolean isStartImmediately() {
        return startImmediately;
    }

    /**
     * 设置是否立即开始
     *
     * @param startImmediately 是否立即开始
     */
    public void setStartImmediately(boolean startImmediately) {
        this.startImmediately = startImmediately;
    }
}