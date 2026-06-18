package com.hyperfetch.event;

/**
 * 错误事件类
 * 用于通知下载过程中发生的错误
 */
public class ErrorEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 错误原因
     */
    private String cause;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 是否可重试
     */
    private boolean retryable;

    /**
     * 异常对象
     */
    private Throwable throwable;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public ErrorEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId     任务ID
     * @param cause      错误原因
     * @param retryCount 重试次数
     */
    public ErrorEvent(String taskId, String cause, int retryCount) {
        this.taskId = taskId;
        this.cause = cause;
        this.retryCount = retryCount;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带异常对象的构造函数
     *
     * @param taskId     任务ID
     * @param cause      错误原因
     * @param retryCount 重试次数
     * @param throwable  异常对象
     */
    public ErrorEvent(String taskId, String cause, int retryCount, Throwable throwable) {
        this.taskId = taskId;
        this.cause = cause;
        this.retryCount = retryCount;
        this.throwable = throwable;
        this.retryable = true;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "taskId='" + taskId + '\'' +
                ", cause='" + cause + '\'' +
                ", retryCount=" + retryCount +
                ", retryable=" + retryable +
                ", timestamp=" + timestamp +
                '}';
    }
}