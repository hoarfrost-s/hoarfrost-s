package com.hyperfetch.repo;

import androidx.room.Entity;

/**
 * 任务分块实体类
 * 用于存储多线程下载时的分块信息
 */
@Entity(tableName = "task_chunks", primaryKeys = {"taskId", "index"})
public class TaskChunkEntity {
    
    /**
     * 所属任务ID
     */
    public String taskId;
    
    /**
     * 分块索引
     */
    public int index;
    
    /**
     * 分块起始位置（字节）
     */
    public long start;
    
    /**
     * 分块结束位置（字节）
     */
    public long end;
    
    /**
     * 已下载大小（字节）
     */
    public long downloaded;
    
    /**
     * 分块状态
     */
    public String status;
}