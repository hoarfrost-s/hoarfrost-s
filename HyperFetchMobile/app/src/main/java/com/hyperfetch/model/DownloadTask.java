package com.hyperfetch.model;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下载任务实体类
 * 包含下载任务的所有信息和状态
 */
public class DownloadTask {

    /**
     * 任务唯一标识
     */
    private volatile String id;

    /**
     * 下载地址
     */
    private volatile String url;

    /**
     * 文件名
     */
    private volatile String fileName;

    /**
     * 文件总大小（字节）
     */
    private volatile long totalSize;

    /**
     * 已下载大小（字节）- 使用AtomicLong保证原子性
     */
    private final AtomicLong downloaded;

    /**
     * 文件分类
     */
    private volatile Category category;

    /**
     * 任务状态
     */
    private volatile TaskStatus status;

    /**
     * 优先级
     */
    private volatile Priority priority;

    /**
     * 线程数
     */
    private volatile int threadCount;

    /**
     * 速度限制（字节/秒）
     */
    private volatile long speedLimit;

    /**
     * 保存路径
     */
    private volatile String savePath;

    /**
     * 创建时间
     */
    private volatile Date createTime;

    /**
     * 默认构造函数
     */
    public DownloadTask() {
        this.downloaded = new AtomicLong(0);
    }

    /**
     * 带参数的构造函数
     *
     * @param id        任务ID
     * @param url       下载地址
     * @param fileName  文件名
     * @param savePath  保存路径
     */
    public DownloadTask(String id, String url, String fileName, String savePath) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.savePath = savePath;
        this.downloaded = new AtomicLong(0);
        this.createTime = new Date();
        this.status = TaskStatus.QUEUED;
        this.priority = Priority.NORMAL;
        this.threadCount = 3;
        this.speedLimit = 0;
        this.category = Category.OTHER;
    }

    // Getter 和 Setter 方法

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloaded() {
        return downloaded.get();
    }

    public void setDownloaded(long downloaded) {
        this.downloaded.set(downloaded);
    }

    /**
     * 原子性地增加已下载大小
     *
     * @param delta 增量
     * @return 更新后的值
     */
    public long addDownloaded(long delta) {
        return downloaded.addAndGet(delta);
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public long getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(long speedLimit) {
        this.speedLimit = speedLimit;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取下载进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public int getProgress() {
        if (totalSize <= 0) {
            return 0;
        }
        return (int) (downloaded.get() * 100 / totalSize);
    }
}