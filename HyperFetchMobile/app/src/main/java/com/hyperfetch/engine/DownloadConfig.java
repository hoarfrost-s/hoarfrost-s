package com.hyperfetch.engine;

/**
 * 下载配置类
 * 包含下载引擎的全局配置参数
 */
public class DownloadConfig {

    /**
     * 默认线程数
     */
    public static final int DEFAULT_THREAD_COUNT = 4;

    /**
     * 最大线程数
     */
    public static final int MAX_THREAD_COUNT = 9;

    /**
     * 最小线程数
     */
    public static final int MIN_THREAD_COUNT = 1;

    /**
     * 默认限速（0表示无限制）
     */
    public static final long DEFAULT_SPEED_LIMIT = 0;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_RETRY_COUNT = 5;

    /**
     * 最大并发任务数
     */
    public static final int MAX_CONCURRENT_TASKS = 5;

    /**
     * 最小并发任务数
     */
    public static final int MIN_CONCURRENT_TASKS = 1;

    /**
     * 默认并发任务数
     */
    public static final int DEFAULT_CONCURRENT_TASKS = 3;

    /**
     * 进度更新间隔（毫秒）
     */
    public static final long PROGRESS_UPDATE_INTERVAL = 1000;

    /**
     * 缓冲区大小（8KB）
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * 连接超时时间（毫秒）
     */
    public static final int CONNECT_TIMEOUT = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    public static final int READ_TIMEOUT = 30000;

    /**
     * 线程数
     */
    private int threadCount;

    /**
     * 限速（字节/秒，0表示无限制）
     */
    private long speedLimit;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大并发任务数
     */
    private int maxConcurrentTasks;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout;

    /**
     * 默认构造函数
     * 使用默认配置值
     */
    public DownloadConfig() {
        this.threadCount = DEFAULT_THREAD_COUNT;
        this.speedLimit = DEFAULT_SPEED_LIMIT;
        this.retryCount = DEFAULT_RETRY_COUNT;
        this.maxConcurrentTasks = DEFAULT_CONCURRENT_TASKS;
        this.connectTimeout = CONNECT_TIMEOUT;
        this.readTimeout = READ_TIMEOUT;
    }

    /**
     * 构造函数
     *
     * @param threadCount 线程数
     */
    public DownloadConfig(int threadCount) {
        this();
        setThreadCount(threadCount);
    }

    // Getter 和 Setter 方法

    public int getThreadCount() {
        return threadCount;
    }

    /**
     * 设置线程数
     * 线程数范围：[MIN_THREAD_COUNT, MAX_THREAD_COUNT]
     *
     * @param threadCount 线程数
     */
    public void setThreadCount(int threadCount) {
        if (threadCount < MIN_THREAD_COUNT) {
            this.threadCount = MIN_THREAD_COUNT;
        } else if (threadCount > MAX_THREAD_COUNT) {
            this.threadCount = MAX_THREAD_COUNT;
        } else {
            this.threadCount = threadCount;
        }
    }

    public long getSpeedLimit() {
        return speedLimit;
    }

    /**
     * 设置限速
     *
     * @param speedLimit 限速（字节/秒），0表示无限制
     */
    public void setSpeedLimit(long speedLimit) {
        this.speedLimit = speedLimit < 0 ? 0 : speedLimit;
    }

    public int getRetryCount() {
        return retryCount;
    }

    /**
     * 设置重试次数
     *
     * @param retryCount 重试次数
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount < 0 ? 0 : retryCount;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    /**
     * 设置最大并发任务数
     * 范围：[MIN_CONCURRENT_TASKS, MAX_CONCURRENT_TASKS]
     *
     * @param maxConcurrentTasks 最大并发任务数
     */
    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        if (maxConcurrentTasks < MIN_CONCURRENT_TASKS) {
            this.maxConcurrentTasks = MIN_CONCURRENT_TASKS;
        } else if (maxConcurrentTasks > MAX_CONCURRENT_TASKS) {
            this.maxConcurrentTasks = MAX_CONCURRENT_TASKS;
        } else {
            this.maxConcurrentTasks = maxConcurrentTasks;
        }
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout > 0 ? connectTimeout : CONNECT_TIMEOUT;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout > 0 ? readTimeout : READ_TIMEOUT;
    }

    /**
     * 获取重试间隔时间（指数退避）
     * 间隔序列：1s, 2s, 4s, 8s, 16s
     *
     * @param retryAttempt 当前重试次数（从0开始）
     * @return 重试间隔时间（毫秒）
     */
    public static long getRetryDelay(int retryAttempt) {
        // 指数退避：1s, 2s, 4s, 8s, 16s
        // 2^retryAttempt * 1000
        return (long) (Math.pow(2, retryAttempt) * 1000);
    }

    @Override
    public String toString() {
        return "DownloadConfig{" +
                "threadCount=" + threadCount +
                ", speedLimit=" + speedLimit +
                ", retryCount=" + retryCount +
                ", maxConcurrentTasks=" + maxConcurrentTasks +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}