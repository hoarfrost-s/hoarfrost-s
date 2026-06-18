package com.hyperfetch.event;

import com.hyperfetch.model.Category;

/**
 * 任务创建事件类
 * 用于通知新任务已创建
 */
public class TaskCreatedEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 下载URL
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 任务分类
     */
    private Category category;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 默认构造函数
     */
    public TaskCreatedEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId   任务ID
     * @param url      下载URL
     * @param fileName 文件名
     * @param category 任务分类
     */
    public TaskCreatedEvent(String taskId, String url, String fileName, Category category) {
        this.taskId = taskId;
        this.url = url;
        this.fileName = fileName;
        this.category = category;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TaskCreatedEvent{" +
                "taskId='" + taskId + '\'' +
                ", url='" + url + '\'' +
                ", fileName='" + fileName + '\'' +
                ", category=" + category +
                ", timestamp=" + timestamp +
                '}';
    }
}