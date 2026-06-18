package com.hyperfetch.repo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 调度器配置实体类
 * 用于存储下载调度器的全局配置
 */
@Entity(tableName = "scheduler_config")
public class ConfigEntity {
    
    /**
     * 配置ID，固定为1
     */
    @PrimaryKey
    public int id;
    
    /**
     * 最大并发任务数
     */
    public int maxConcurrentTasks;
    
    /**
     * 是否启用多任务
     */
    public boolean multiTaskEnabled;
    
    /**
     * 队列策略
     */
    public String queueStrategy;
    
    /**
     * 是否仅WiFi下载
     */
    public boolean wifiOnly;
    
    /**
     * 默认线程数
     */
    public int defaultThreadCount;
}