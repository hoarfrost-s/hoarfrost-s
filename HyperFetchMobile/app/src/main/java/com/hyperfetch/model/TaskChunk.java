package com.hyperfetch.model;

/**
 * 任务分块实体类
 * 用于多线程下载时的分块管理
 */
public class TaskChunk {

    /**
     * 所属任务ID
     */
    private String taskId;

    /**
     * 分块索引
     */
    private int index;

    /**
     * 分块起始位置（字节）
     */
    private long start;

    /**
     * 分块结束位置（字节）
     */
    private long end;

    /**
     * 已下载大小（字节）
     */
    private long downloaded;

    /**
     * 分块状态
     */
    private ChunkStatus status;

    /**
     * 默认构造函数
     */
    public TaskChunk() {
        this.downloaded = 0;
        this.status = ChunkStatus.PENDING;
    }

    /**
     * 带参数的构造函数
     *
     * @param taskId 所属任务ID
     * @param index  分块索引
     * @param start  起始位置
     * @param end    结束位置
     */
    public TaskChunk(String taskId, int index, long start, long end) {
        this.taskId = taskId;
        this.index = index;
        this.start = start;
        this.end = end;
        this.downloaded = 0;
        this.status = ChunkStatus.PENDING;
    }

    // Getter 和 Setter 方法

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public ChunkStatus getStatus() {
        return status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
    }

    /**
     * 获取分块大小
     *
     * @return 分块大小（字节）
     */
    public long getChunkSize() {
        return end - start + 1;
    }

    /**
     * 获取分块进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public int getProgress() {
        long chunkSize = getChunkSize();
        if (chunkSize <= 0) {
            return 0;
        }
        return (int) (downloaded * 100 / chunkSize);
    }
}