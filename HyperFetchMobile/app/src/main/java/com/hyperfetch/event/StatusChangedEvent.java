package com.hyperfetch.event;

import com.hyperfetch.model.TaskStatus;

/**
 * 状态变更事件类
 * 用于通知任务状态发生改变
 */
public class StatusChangedEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 原状态
     */
    private TaskStatus fromStatus;

    /**
     * 新状态
     */
    private TaskStatus toStatus;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public StatusChangedEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId     任务ID
     * @param fromStatus 原状态
     * @param toStatus   新状态
     */
    public StatusChangedEvent(String taskId, TaskStatus fromStatus, TaskStatus toStatus) {
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(TaskStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public TaskStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TaskStatus toStatus) {
        this.toStatus = toStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StatusChangedEvent{" +
                "taskId='" + taskId + '\'' +
                ", fromStatus=" + fromStatus +
                ", toStatus=" + toStatus +
                ", timestamp=" + timestamp +
                '}';
    }
}