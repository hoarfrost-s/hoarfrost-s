package com.hyperfetch.event;

/**
 * 完成事件类
 * 用于通知任务下载完成
 */
public class CompletedEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件保存路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 总耗时（毫秒）
     */
    private long totalTime;

    /**
     * 平均下载速度（字节/秒）
     */
    private long averageSpeed;

    /**
     * 是否有警告（如合并失败但保留了原始文件）
     */
    private boolean hasWarning;

    /**
     * 警告信息
     */
    private String warningMessage;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public CompletedEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId   任务ID
     * @param filePath 文件保存路径
     */
    public CompletedEvent(String taskId, String filePath) {
        this.taskId = taskId;
        this.filePath = filePath;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 完整参数的构造函数
     *
     * @param taskId        任务ID
     * @param filePath      文件保存路径
     * @param fileSize      文件大小
     * @param totalTime     总耗时
     * @param averageSpeed  平均下载速度
     */
    public CompletedEvent(String taskId, String filePath, long fileSize, long totalTime, long averageSpeed) {
        this.taskId = taskId;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.totalTime = totalTime;
        this.averageSpeed = averageSpeed;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(long averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public boolean isHasWarning() {
        return hasWarning;
    }

    public void setHasWarning(boolean hasWarning) {
        this.hasWarning = hasWarning;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CompletedEvent{" +
                "taskId='" + taskId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", totalTime=" + totalTime +
                ", averageSpeed=" + averageSpeed +
                ", hasWarning=" + hasWarning +
                ", timestamp=" + timestamp +
                '}';
    }
}