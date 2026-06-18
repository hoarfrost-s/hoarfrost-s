package com.hyperfetch.event;

/**
 * 进度事件类
 * 用于发布下载进度更新通知
 */
public class ProgressEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 已下载字节数
     */
    private long downloaded;

    /**
     * 总字节数
     */
    private long totalSize;

    /**
     * 当前下载速度（字节/秒）
     */
    private long speed;

    /**
     * 下载进度百分比（0-100）
     */
    private int progress;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public ProgressEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId     任务ID
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     * @param speed      当前下载速度
     */
    public ProgressEvent(String taskId, long downloaded, long totalSize, long speed) {
        this.taskId = taskId;
        this.downloaded = downloaded;
        this.totalSize = totalSize;
        this.speed = speed;
        this.progress = calculateProgress(downloaded, totalSize);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 计算进度百分比
     *
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     * @return 进度百分比（0-100）
     */
    private int calculateProgress(long downloaded, long totalSize) {
        if (totalSize <= 0) {
            return 0;
        }
        return (int) (downloaded * 100 / totalSize);
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ProgressEvent{" +
                "taskId='" + taskId + '\'' +
                ", downloaded=" + downloaded +
                ", totalSize=" + totalSize +
                ", speed=" + speed +
                ", progress=" + progress +
                ", timestamp=" + timestamp +
                '}';
    }
}